#version 330

// Use #defines to compile separate shaders for each projection type
#define PROJECTION_MAXIMUM 0
#define PROJECTION_OCCLUDING 1
#define PROJECTION_ISOSURFACE 2
// REPLACE THE FOLLOWING LINE WITH CORRECT PROJECTION DEFINE
#define PROJECTION_MODE PROJECTION_MAXIMUM

/**
 * Ray casting 3D Maximum Intensity Projection shader.
 * This is my first ray casting shader, but I expect many more
 * sophisticated ones to follow, once this is debugged.
 */

// output 1) maximum intensity along ray in red channel
//        2) relative depth of brightest voxel in green channel
layout(location = 0) out vec4 colorOut; 

// put surface normals in eye space for isosurface projection
uniform mat4 tcToCamera = mat4(1);

// clip using depth buffer from opaque pass
uniform sampler2D opaqueDepthTexture;
uniform vec2 opaqueZNearFar = vec2(1e-2, 1e4);

// additional render target for picking
// TODO: Remove pick part, until it is actually needed
layout(location = 1) out ivec2 pickId;
uniform int pickIndex = 3; // default value for pick buffer

// Mouse Light is restricted to two color channels for the forseeable future
#define COLOR_VEC vec2

// Opacity component of transfer function
// NOTE: Ensure that these values are consistent with those from downstream contrast shader.
// (it might be OK for these to include a broader range; at least for MIP).
uniform COLOR_VEC opacityFunctionMin = COLOR_VEC(0);
uniform COLOR_VEC opacityFunctionMax = COLOR_VEC(1);

uniform vec3 camPosInTc; // camera position, in texture coordinate frame
uniform sampler3D volumeTexture; // the confocal image stack
uniform int levelOfDetail = 0; // volume texture LOD

// Use actual voxelSize, as uniform, for more accurate occlusion
uniform vec3 volumeMicrometers = vec3(256, 256, 200);
// TODO - expose occluding path length to user
uniform float canonicalOccludingPathLengthUm = 1.0; // micrometers

// Homogeneous clip plane equations, in texture coordinates
uniform vec4 nearSlabPlane; // for limiting view to a screen-parallel slab
uniform vec4 farSlabPlane; // for limiting view to a screen-parallel slab

// TODO: - set filtering dynamically depending on user interaction.
#define FILTER_NEAREST 0
#define FILTER_TRILINEAR 1
#define FILTER_TRICUBIC 3 // Expensive beautiful rendering option, for slow, high quality rendering passes
uniform int filteringOrder = 3; // 0: NEAREST; 1: TRILINEAR; 2: <not used> 3: TRICUBIC

in vec3 fragTexCoord; // texture coordinate at mesh surface of volume

// Helper method for tricubic filtering
// From http://stackoverflow.com/questions/13501081/efficient-bicubic-filtering-code-in-glsl
// B-spline cubic smooths out actual values...
vec4 cubic_bspline(float x)
{
    float x2 = x * x;
    float x3 = x2 * x;
    vec4 w;
    w.x =   -x3 + 3*x2 - 3*x + 1;
    w.y =  3*x3 - 6*x2       + 4;
    w.z = -3*x3 + 3*x2 + 3*x + 1;
    w.w =  x3;
    return w / 6.f;
}

// Catmull-Rom spline actually passes through control points
vec4 cubic(float x) // cubic_catmullrom(float x)
{
    const float s = 0.5; // potentially adjustable parameter
    float x2 = x * x;
    float x3 = x2 * x;
    vec4 w;
    w.x =    -s*x3 +     2*s*x2 - s*x + 0;
    w.y = (2-s)*x3 +   (s-3)*x2       + 1;
    w.z = (s-2)*x3 + (3-2*s)*x2 + s*x + 0;
    w.w =     s*x3 -       s*x2       + 0;
    return w;
}

// Fast cubic interpolation using a source that is already linearly interpolated.
//   @param texture 3D texture to sample
//   @param texcoord 3D texture coordinate of point to sample, in non-normalized texel units
//   @param texscale 1/texelDimension of texture
//   @param lod level of detail mipmap texture level to use for sampling
// Adapted from https://groups.google.com/forum/#!topic/comp.graphics.api.opengl/kqrujgJfTxo
COLOR_VEC filterFastCubic3D(sampler3D texture, vec3 texcoord, vec3 texscale, int lod)
{
    // Compute local texture coordinates, normalized within the current voxel
    float fx = fract(texcoord.x);
    float fy = fract(texcoord.y);
    float fz = fract(texcoord.y);

    // Compute texture coordinates of voxel corner
    texcoord.x -= fx;
    texcoord.y -= fy;
    texcoord.z -= fz;

    // Compute 4 cubic spline coefficients for each principal dimension
    vec4 xcubic = cubic(fx);
    vec4 ycubic = cubic(fy);
    vec4 zcubic = cubic(fz);

    // This is 3D version of original 2D code sample
    vec3 c0 = texcoord - vec3(0.5, 0.5, 0.5);
    vec3 s0 = vec3(xcubic.x + xcubic.y, ycubic.x + ycubic.y, zcubic.x + zcubic.y);
    vec3 offset0 = c0 + vec3(xcubic.y, ycubic.y, zcubic.y) / s0;

    // For performance, interleave texture fetches with arithmetic
    // fetch...
    COLOR_VEC sample000 = COLOR_VEC(textureLod(texture, vec3(offset0.x, offset0.y, offset0.z) * texscale, lod));
    // compute...
    vec3 c1 = texcoord + vec3(1.5, 1.5, 1.5);
    vec3 s1 = vec3(xcubic.z + xcubic.w, ycubic.z + ycubic.w, zcubic.z + zcubic.w);
    vec3 offset1 = c1 + vec3(xcubic.w, ycubic.w, zcubic.w) / s1;
    // fetch...
    COLOR_VEC sample100 = COLOR_VEC(textureLod(texture, vec3(offset1.x, offset0.y, offset0.z) * texscale, lod));
    // compute...
    float sx = s0.x / (s0.x + s1.x);
    COLOR_VEC sampleX00 = mix(sample100, sample000, sx);
    // fetch...
    COLOR_VEC sample010 = COLOR_VEC(textureLod(texture, vec3(offset0.x, offset1.y, offset0.z) * texscale, lod));
    COLOR_VEC sample110 = COLOR_VEC(textureLod(texture, vec3(offset1.x, offset1.y, offset0.z) * texscale, lod));
    // compute...
    float sy = s0.y / (s0.y + s1.y);
    COLOR_VEC sampleX10 = mix(sample110, sample010, sx);
    COLOR_VEC sampleXY0 = mix(sampleX10, sampleX00, sy);
    // fetch...
    COLOR_VEC sample001 = COLOR_VEC(textureLod(texture, vec3(offset0.x, offset0.y, offset1.z) * texscale, lod));
    COLOR_VEC sample101 = COLOR_VEC(textureLod(texture, vec3(offset1.x, offset0.y, offset1.z) * texscale, lod));
    // compute...
    COLOR_VEC sampleX01 = mix(sample101, sample001, sx);
    float sz = s0.z / (s0.z + s1.z);
    // final fetch.
    COLOR_VEC sample011 = COLOR_VEC(textureLod(texture, vec3(offset0.x, offset1.y, offset1.z) * texscale, lod));
    COLOR_VEC sample111 = COLOR_VEC(textureLod(texture, vec3(offset1.x, offset1.y, offset1.z) * texscale, lod));

    // compute.
    COLOR_VEC sampleX11 = mix(sample111, sample011, sx);
    COLOR_VEC sampleXY1 = mix(sampleX11, sampleX01, sy);

    // Interpolate 8 samples
    return mix(sampleXY1, sampleXY0, sz);
}

// convert hue-saturation-value color, to red-green-blue
vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

// Return min/max ray parameters to keep in unit volume: x/y/z in (0,1)
vec2 clipRayToUnitVolume(vec3 x0, vec3 x1) {
    // Compute extreme texture coordinates
    // Texture coordinates are restricted to [0,1]
    // The texMax/texMin variables represent texture coordinates corresponding to 
    // large/small values of ray parameter "t".
    vec3 texMax = clamp(ceil(x1 * 100), vec3(0,0,0), vec3(1,1,1)); // each component is now 0 or 1
    vec3 texMin = vec3(1,1,1) - texMax; // complement texMax
    // looks OK
    // colorOut = vec4(texMin, 1); return; // for debugging
    // colorOut = vec4(texMax, 1); return; // for debugging

    // Compute parameter t limits in all three directions at once, 
    // using plane ray tracing equation.
    vec3 vtMin = -(x0 - texMin)/x1;
    float tMin = max(max(vtMin.x, vtMin.y), vtMin.z); // looks OK
    vec3 vtMax = -(x0 - texMax)/x1;
    float tMax = min(min(vtMax.x, vtMax.y), vtMax.z); // OK?
    // colorOut = vec4(-vtMin, 1); return; // for debugging, should be non-postitive everywhere
    // colorOut = vec4(vtMax, 1); return; // for debugging, should be non-negative everywhere

    float tRange = tMax - tMin; // maximum ray length
    // colorOut = vec4(tRange, 0, 0, 1); return; // for debugging
    return vec2(tMin, tMax);
}

float rampstep(float edge0, float edge1, float x) {
    return clamp((x - edge0)/(edge1 - edge0), 0.0, 1.0);
}

vec2 rampstep(vec2 edge0, vec2 edge1, vec2 x) {
    return clamp((x - edge0)/(edge1 - edge0), 0.0, 1.0);
}

vec4 rampstep(vec4 edge0, vec4 edge1, vec4 x) {
    return clamp((x - edge0)/(edge1 - edge0), 0.0, 1.0);
}

vec4 rampstepNoClamp(vec4 edge0, vec4 edge1, vec4 x) {
    return (x - edge0)/(edge1 - edge0);
}

vec3 rampstep(vec3 edge0, vec3 edge1, vec3 x) {
    return clamp((x - edge0)/(edge1 - edge0), 0.0, 1.0);
}

float maxElement(vec2 v) {
    return max(v.r, v.g);
}

float maxElement(vec3 v) {
    return max( max(v.r, v.g), v.b);
}

float maxElement(vec4 v) {
    return max( max(v.r, v.g), max(v.b, v.a) );
}

float minElement(vec2 v) {
    return min(v.r, v.g);
}

float minElement(vec4 v) {
    return min( min(v.r, v.g), min(v.b, v.a) );
}

float sampleScaledIntensity(sampler3D volume, vec3 uvw, vec3 textureScale) 
{
    COLOR_VEC unscaled;
    if (filteringOrder == 3)
        unscaled = filterFastCubic3D(volume, uvw, textureScale, levelOfDetail);
    else
        unscaled = COLOR_VEC(textureLod(volume, uvw*textureScale, levelOfDetail));
    return maxElement( unscaled );
}

vec3 calculateNormalInScreenSpace(sampler3D volume, vec3 uvw, vec3 voxelMicrometers, vec3 textureScale) 
{
    vec3 v[2];
    const float delta = 0.75; // sample 1/2 pixel away

    // 0.2 *, to avoid overflow
    const float downScale = 0.4;
    v[0].x = downScale * sampleScaledIntensity(volume, uvw + vec3(delta, 0, 0), textureScale);
    v[1].x = downScale * sampleScaledIntensity(volume, uvw - vec3(delta, 0, 0), textureScale);

    v[0].y = downScale * sampleScaledIntensity(volume, uvw + vec3(0, delta, 0), textureScale);
    v[1].y = downScale * sampleScaledIntensity(volume, uvw - vec3(0, delta, 0), textureScale);

    v[0].z = downScale * sampleScaledIntensity(volume, uvw + vec3(0, 0, delta), textureScale);
    v[1].z = downScale * sampleScaledIntensity(volume, uvw - vec3(0, 0, delta), textureScale);

    vec3 result = normalize( (v[1] - v[0]) );
    result = (tcToCamera * vec4(result, 0)).xyz; // TODO should be matrix that converts TC to camera
    return normalize(result);
}

// Old main function, before refactoring January 2016
void main0() {
    COLOR_VEC vecIntegratedIntensity = COLOR_VEC(0); // up to 2 color channels
    float integratedOpacity = 0;

    // Set up ray equation
    vec3 x0 = fragTexCoord; // origin, at first intersection of view ray with surface
    vec3 viewDirection = normalize(fragTexCoord - camPosInTc);
    vec3 x1 = viewDirection; // view/ray direction

    // colorOut = vec4(0.5*(x1+vec3(1,1,1)), 1); return; // for debugging
    // colorOut = vec4(x0, 1); return; // for debugging

    // Compute bounds on ray parameter
    vec2 tMinMax = clipRayToUnitVolume(x0, x1);

    // Clamp ray to near/far clip planes of view slab
    // (this is smaller and distinct from perspective projection zNear zFar)
    float tMinSlab = -dot(nearSlabPlane, vec4(x0,1)) / dot(nearSlabPlane, vec4(x1,0));
    tMinMax.x = max(tMinSlab, tMinMax.x);
    float tMaxSlab = -dot(farSlabPlane, vec4(x0,1)) / dot(farSlabPlane, vec4(x1,0));
    tMinMax.y = min(tMaxSlab, tMinMax.y);

    // Clip by depth buffer from opaque render pass
    // http://web.archive.org/web/20130416194336/http://olivers.posterous.com/linear-depth-in-glsl-for-real
    // next line assumes that opaqueDepthTexture is the same size as the current viewport
    vec2 depthTc = gl_FragCoord.xy / textureSize(opaqueDepthTexture, 0); // compute texture coordinate for depth lookup
    float z_buf = texture(opaqueDepthTexture, depthTc).x; // raw depth value from z-buffer
    float zNear = opaqueZNearFar.x;
    float zFar = opaqueZNearFar.y;
    float z_eye = 2*zFar*zNear / (zFar + zNear - (zFar - zNear)*(2*z_buf - 1));
    vec4 depth_plane_eye = vec4(0, 0, 1, z_eye);
    vec4 depth_plane_tc = transpose(tcToCamera)*depth_plane_eye; // it's complicated...
    float tDepth = -dot(depth_plane_tc, vec4(x0,1)) / dot(depth_plane_tc, vec4(x1,0));
    // z_buf tends to be zero when depth texture is uninitialized
    // I hope valid zero values are uncommon...
    // z_buf should be 1.0 where there was no opaque geometry, so skip those too.
    if ((z_buf != 0) && (z_buf < 0.9999)) {
        tMinMax.y = min(tDepth, tMinMax.y);
    }

    // color by level-of-detail (for debugging only)
    vec3 color = vec3(1,1,1); // default color is white
    const bool colorByLod = false;
    if (colorByLod) {
        // different hue for each level-of-detail
        // 0 -> red; maximum detail
        // 1 -> yellow 
        // 2 -> green
        // 3 -> cyan
        // 4 -> blue
        // 5 -> magenta
        // 6 -> red (cycle repeats)
        // etc.
        vec3 hsv = vec3(levelOfDetail/6.0, 0.8, 1); 
        color = hsv2rgb(hsv);
    }

    // Propagate ray from front surface to rear surface
    // Every step, always advance some finite amount
    // We can take bigger steps, if looking down a coarse dimension
    // How big is a typical voxel along the ray?
    ivec3 volumeSize = textureSize(volumeTexture, levelOfDetail);
    vec3 textureScale = vec3(1,1,1)/volumeSize;
    float voxelStep = dot(textureScale, abs(viewDirection));

    // Use absolute scale for occluding length parameter
    // TODO - express length parameter in micrometers...
    ivec3 finestVolumeSize = textureSize(volumeTexture, 0);
    // Precompute conversion from ray segment length to transparency exponent
    float segmentLengthFactor = dot(volumeMicrometers, abs(x1));
    segmentLengthFactor /= pow(2.0, float(levelOfDetail)); // Try to reduce LOD popping
    segmentLengthFactor /= canonicalOccludingPathLengthUm;

    // How small a corner are we willing to precisely sample?
    // smaller value => sharper corners/slower progress
    // 0.50 shows terrible artifacts
    // 0.25 shows subtle artifacts
    // 0.05 shows subtle artifacts
    // NOTE: minStep is the *minimum* step size, not THE step size, which will
    // typically be roughly voxelStep.
    float minStep = 0.01 * voxelStep;
    vec3 forwardMask = ceil(x1 * 0.99); // each component is now 0 or 1

    // Convert ray equation parameters from normalized texture coordinates
    //  to texels, to minimize arithmetic in main loop below.
    x0 *= volumeSize;
    x1 *= volumeSize;

    float previousEdge = tMinMax.x; // track upstream voxel edge

    #if PROJECTION_MODE == PROJECTION_ISOSURFACE
        COLOR_VEC isoThreshold = 0.5 * (opacityFunctionMin + opacityFunctionMax);
        // COLOR_VEC isoThreshold = opacityFunctionMin;
        float previousThreshDist = 0;
        float previousT = previousEdge;
    #endif

    // TODO: store results of ray casting at multiple positions:
    float tMaxAbs = previousEdge; // ray point where intensity is at local maximum after tThreshold
    // TODO - implement these other values, if needed
    // float tThreshold = -1; // ray point where intensity first exceeds opacityFunctionMax
    // float tMaxGrad = previousEdge; // ray point where gradient is local maximum before tMaxAbs
    // float tMaxRel = previousEdge; // ray point where maximum contribution to blended opacity is made

    bool hasHitThreshold = false;
    float previousOpacity = minElement(opacityFunctionMin);
    float dIntensity = 0;
    // float edgeFade = 1.0; // default to no fading
    // float fadedIntensity = 0.0;

    // 
    vec3 rayBoxCorner;
    if (filteringOrder == 0)  // nearest neighbor
    {
        rayBoxCorner = vec3(0, 0, 0); // intersect ray at voxel edge planes, optimal for nearest-neighbor
    } 
    else  // trilinear and tricubic
    {
        rayBoxCorner = 
                vec3(0.5, 0.5, 0.5); // intersect ray at voxel center planes; trilinear occluded: two-tone edges and gradient faces, but best overall?
                // vec3(0.0, 0.0, 0.0); // intersect ray at voxel edge planes; trilinear occluded: faces/edges too bright
                // vec3(0.6, 0.6, 0.6); // slightly off-center is not better
    }

    // variables used to find local max intensity in occluding projection
    float maxDI = 0; // most occluding dAlpha "most visible point"
    float bodyMaxI = 0; // brightest intensity past maxDI
    // Store two locations, so we could center on median location of saturated values
    float tFirstBodyMaxI; // first location of bodyMaxI
    float tFinalBodyMaxI; // last location of bodyMaxI
    bool inLocalBody = false;

    float previousDeltaT = 0;

    bool sampleSegmentCenter = false;
    if (filteringOrder == 0) {
        sampleSegmentCenter = true;
    }

    const int maxSteps = 500;
    int stepCount = 0;
    // BEGIN SLOW PART BELOW - focus optimizations here!
    // This loop could execute hundreds or thousands of times
    // Visit each intersected voxel along the view ray exactly once.
    while (previousEdge <= tMinMax.y) {
        stepCount += 1;
        if (stepCount > maxSteps) break;
        // Advance ray by at least minStep, to avoid getting stuck in tiny corners
        float t = previousEdge + minStep;
        vec3 currentTexelPos = (x0 + t*x1); // apply ray equation to find new voxel

        // Advance ray to next voxel edge.
        // For NEAREST filter, advance to midplanes between voxel centers.
        // For TRILINEAR and TRICUBIC filters, advance to planes connecing voxel centers.
        vec3 currentTexel = floor(currentTexelPos + rayBoxCorner) - rayBoxCorner;

        // Three out of six total voxel edges represent forward progress
        vec3 candidateEdges = currentTexel + forwardMask;
        // Ray trace to three planar voxel edges at once.
        vec3 candidateSteps = -(x0 - candidateEdges)/x1;
        // Clamp to reasonable range
        vec3 minT = vec3(t,t,t); // previous step plus some
        candidateSteps = clamp(candidateSteps, minT, minT + 2.0 * vec3(1,1,1)); // does not remove corduroy effect
        // Choose the closest voxel edge.
        float nextEdge = min(candidateSteps.x, min(candidateSteps.y, candidateSteps.z));
        // Advance ray by at least minStep, to avoid getting stuck in tiny corners
        // Next line should be unneccessary, but prevents (sporadic?) driver crash
        nextEdge = max(nextEdge, previousEdge + minStep);

        // At the end of the ray, sample the very end, for best depth clipping
        if ((nextEdge > tMinMax.y) && (previousEdge < tMinMax.y))
            nextEdge = tMinMax.y - minStep/10;

        // Average between previousEdge and nextEdge only for NEAREST filtering...
        // Sample ray at voxel center (midpoint between voxel edge intersections)
        // if (true) {
        if (sampleSegmentCenter) {
            t = mix(previousEdge, nextEdge, 0.5); // voxel center
        }
        else {
            t = previousEdge; // sample voxel trailing edge
        }

        float deltaT = nextEdge - previousEdge;
        float segmentLength = 
                0.5 * (deltaT + previousDeltaT); // should be correct for trailing edge sample
        if (sampleSegmentCenter) {
            segmentLength = deltaT; // perfect for nearest neighbor
        }

        // Update before next iteration; AND before any shortcuts...
        float cachedPreviousEdge = previousEdge; // for isosurface nearest neighbor surface location
        previousEdge = nextEdge;
        previousDeltaT = deltaT;

        // shortcut
        if (segmentLength <= 0) continue;

        // Apply ray equation to compute texel coordinate from ray parameter.
        currentTexelPos = (x0 + t*x1); 
        vec3 texCoord = currentTexelPos * textureScale; // converted back to normalized texture coordinates,
        // Fetch texture intensity (EXPENSIVE!)
        COLOR_VEC vecLocalIntensity = COLOR_VEC(0);
        if (filteringOrder == 3) {
            // slow tricubic filtering
            vecLocalIntensity = filterFastCubic3D(volumeTexture, currentTexelPos, textureScale, levelOfDetail);
        }
        else {
            // fast linear or nearest-neighbor filtering
            vecLocalIntensity = COLOR_VEC(textureLod(volumeTexture, texCoord, levelOfDetail));
        }

        // compute scalar proxy for intensity
        float localOpacity = maxElement(rampstep(opacityFunctionMin, opacityFunctionMax, vecLocalIntensity));

        // shortcut - skip sufficiently dim intensities
        if (localOpacity <= 0) {
            #if PROJECTION_MODE == PROJECTION_ISOSURFACE
                // Update ray parameters for isosurface, even in case of shortcut
                previousThreshDist = maxElement(vecLocalIntensity - isoThreshold); // should be negative...
                previousT = t;
            #endif
            continue;
        }

        // for occluding projection, incorporation path length into opacity exponent
        #if PROJECTION_MODE == PROJECTION_OCCLUDING
            // Convert segmentLength to micrometers, from ray parameter coordinates
            float transparencyExponent = segmentLength * segmentLengthFactor;
            float localTransparency = 1.0 - localOpacity;
            localTransparency = pow(localTransparency, transparencyExponent); // TODO - is this exponential slow?
            localOpacity = 1.0 - localTransparency;
        #endif

        // Compute change in intensity
        dIntensity = localOpacity - previousOpacity;
        previousOpacity = localOpacity;

        // shortcut - early termination after exceeding threshold AND finding local maximum
        if (hasHitThreshold && (dIntensity < 0)) {
            break;
        }

        // fade intensity at front and back, for smoother clipping
        // NOTE - this fading effect is nice because it avoids popping when objects
        // enter and leave the view slab.
        // BUT the fading effect has performance consequences:
        //  * obviously it adds computations to the slow loop
        //  * ...including the early computation of relative depth
        //  * it also uses the OpacityFunction, which is otherwise not strictly
        //    needed in this shader at all. So, for example, without the fade
        //    effect, this entire render pass could be cached and skipped, as
        //    the user drags the opacity parameters. 
        float rd = (t - tMinSlab) / (tMaxSlab - tMinSlab); // relative depth
        const float fadeBuffer = 0.20; // how much of depth slab to include in fading
        float fade = rampstep(0.0, fadeBuffer, rd);
        fade = min(fade, rampstep(1.0, 1.0 - fadeBuffer, rd));

        localOpacity *= fade;

        // #ifdef projectionMode into multiple shaders...
        #if PROJECTION_MODE == PROJECTION_MAXIMUM
            // Should maximum intensity be per-channel, or per color?
            // Well, per-color allows a distinct XYZ maximum position...
            // vecLocalIntensity = mix(opacityFunctionMin, vecLocalIntensity, fade);
            // Blend per-channel...
            // ...but store XYZ peak per voxel.
            if  (localOpacity > integratedOpacity) {
                vecIntegratedIntensity = vecLocalIntensity;
                integratedOpacity = localOpacity;
                // Prepare to follow ray to brightest spot
                inLocalBody = true;
                bodyMaxI = localOpacity;
                tFirstBodyMaxI = tFinalBodyMaxI = tMaxAbs = t;
            }

            // Walk to median location of saturated segments, for accurate centering
            else if (inLocalBody) {
                if (localOpacity == bodyMaxI) { // local maximum plateau
                    tFinalBodyMaxI = t;
                    tMaxAbs = 0.5 * (tFirstBodyMaxI + tFinalBodyMaxI); // median location of plateau
                }
                else if (localOpacity > bodyMaxI) { // new local maximum
                    tFirstBodyMaxI = t;
                    tFinalBodyMaxI = t;
                    tMaxAbs = t;                    
                    bodyMaxI = localOpacity;
                }
                else {
                    inLocalBody = false; // past local maximum
                }
            }


        #elif PROJECTION_MODE == PROJECTION_OCCLUDING
            // vec4 c_src = mix(opacityFunctionMin, vecLocalIntensity, fade);
            COLOR_VEC c_src = vecLocalIntensity;
            float a_src = localOpacity;

            // Previous integrated values are in FRONT of new values
            COLOR_VEC c_dest = vecIntegratedIntensity;
            float a_dest = integratedOpacity;

            float a_out = 1.0 - (1.0 - a_src)*(1.0 - a_dest);
            COLOR_VEC c_out = c_dest*a_dest/a_out + c_src*(1.0 - a_dest/a_out);
            // float a_out = a_dest + (1 - a_dest)*(a_src); // EQUIVALENT TO ABOVE
            // float c_out = c_dest + (1 - a_dest)*a_src*c_src;

            vecIntegratedIntensity = clamp(c_out, 0, 1); // clamp required to avoid black artifacts
            integratedOpacity = a_out;

            // Update brightest point along ray
            // TODO - click-to-center is not perfect yet...
            float dI = a_src * (1.0 - a_dest/a_out); 
            if (dI > maxDI) { // found new biggest occluder
                maxDI = dI;
                // Prepare to follow ray to brightest spot
                inLocalBody = true;
                bodyMaxI = localOpacity;
                tFirstBodyMaxI = t;
                tFinalBodyMaxI = t;
                tMaxAbs = t;
            }

            // Walk to median location of saturated segments, for accurate centering
            else if (inLocalBody) {
                if (localOpacity == bodyMaxI) { // local maximum plateau
                    tFinalBodyMaxI = t;
                    tMaxAbs = 0.5 * (tFirstBodyMaxI + tFinalBodyMaxI); // median location of plateau
                }
                else if (localOpacity > bodyMaxI) { // new local maximum
                    tFirstBodyMaxI = t;
                    tFinalBodyMaxI = t;
                    tMaxAbs = t;
                    bodyMaxI = localOpacity;
                }
                else {
                    inLocalBody = false; // past local maximum
                }
            }
            
        #else // isosurface
            // Use only first 3(or 2) channels, since alpha is a headache when I want to see negative components.
            float threshDist = maxElement(vecLocalIntensity - isoThreshold);
            if (threshDist > 0) // surface intersected
            {
                // vecIntegratedIntensity = vecLocalIntensity;
                integratedOpacity = 1.0;

                if (filteringOrder == 0) { // nearest neighbor
                    tMaxAbs = cachedPreviousEdge; // retreat to voxel surface
                    vecIntegratedIntensity = vecLocalIntensity; // using intensity of voxel center
                }
                else { // interpolate surface location using measured intensities, to get closer to true isosurface
                    float alpha = (-previousThreshDist) / (threshDist - previousThreshDist);
                    tMaxAbs = mix(previousT, t, alpha);
                    // Resample intensity at interpolated location
                    // (should be very close to threshold intensity...)
                    // But not for nearest neighbor...
                    vec3 currentTexelPos2 = (x0 + tMaxAbs*x1); 
                    vec3 texCoord2 = currentTexelPos2 * textureScale; // converted back to normalized texture coordinates,
                    if (filteringOrder == 3) {
                        // slow tricubic filtering
                        vecIntegratedIntensity = filterFastCubic3D(volumeTexture, currentTexelPos2, textureScale, levelOfDetail);
                    }
                    else {
                        // fast linear or nearest-neighbor filtering
                        vecIntegratedIntensity = COLOR_VEC(textureLod(volumeTexture, texCoord2, levelOfDetail));
                    }
                    // vecIntegratedIntensity += vec4(0.05, 0.05, 0.05, 0); // brighten it up a little...
                }
                break; // stop casting! we found the surface along this ray
            }
            previousThreshDist = threshDist; // negative value
            previousT = t;
        #endif

        if ( maxElement(vecIntegratedIntensity - opacityFunctionMax) > 0 ) {
            // hasHitThreshold = true;
            // break; // early termination - moved to local maximum extension, above
        }
        if (integratedOpacity > 0.99) {
            hasHitThreshold = true;
        }
    }
    // END SLOW PART ABOVE

    // Draw nothing at all, in cases of low maximum intensity
    if ( integratedOpacity <= 0 ) // <= opacityFunction.x) 
    {
        discard;
        return;
    }

    // vec3 c = color*integratedIntensity;
    // #if PROJECTION_MODE == PROJECTION_ISOSURFACE
    // TODO - put normals in a separate frame buffer
    if (false) {
        vec3 uvw = x0 + tMaxAbs * x1;
        vec3 voxelMicrometers = volumeMicrometers / volumeSize;
        vec3 normal = calculateNormalInScreenSpace(
                volumeTexture, 
                uvw, 
                voxelMicrometers, 
                textureScale);
        // Clip normal at front of slab
        if (tMaxAbs <= tMinSlab) normal = vec3(0, 0, 1);
        normal = 0.5 * (normal + vec3(1, 1, 1)); // restrict to range 0-1
        colorOut = vec4(normal.xyz, 1.0);
    }
    else {
        // TODO: coreIntensity in third position
        colorOut = vec4(vecIntegratedIntensity, 0, integratedOpacity);
    }

    // pick value to alternate render target
    float relativeDepth = (tMaxAbs - tMinSlab) / (tMaxSlab - tMinSlab);
    pickId = ivec2(pickIndex, int(relativeDepth * 65535));

    // TODO gl_FragDepth for isosurface
}


// Potential refactoring for better compartmentalized logic Jan 2016

// Data Structures

// For assisted manual neuron tracing, we want to know the "core" intensity
// at the bright center of a neurite, in addition to the "rendered" intensity,
// which could be dimmer, and near the surface of occluding and isosurface
// projections.
// In the case of MIP, the core intensity is the maximum intensity in the 
// tracing channel. [right?]
// In the case of Occluding Projection, the core intensity is the maximum
// tracing-channel intensity in a non-decreasing path past the maximum-opacity
// voxel.

// Data used for finding bright center of neurite for interactive tracing
struct CoreStatus
{
    bool inLocalBody; // TRUE if ray is currently in a bright body
    // float bodyMaxIntensity; // Brightest intensity yet seen in body
    float firstBodyRayParam; // Ray parameter where brightest intensity was first observed
    float finalBodyRayParam; // Ray parameter where brightest intensity was last observed
};

// Accumulated color and data values, for eventual display
struct IntegratedIntensity
{
    COLOR_VEC intensity;
    float opacity;
    float coreIntensity; // Brightest intensity yet seen in tracing channel
    float coreRayParameter; // location of coreIntensity
};

// Precomputed extremes for managing ray casting
struct RayBounds
{
    float minRayParameter;
    float maxRayParameter;
};

// Ray parameters for computation along the view ray
struct RayParameters
{
    vec3 rayOriginInTexels; // usually on the near face of bounding geometry, closest to viewer
    vec3 rayDirectionInTexels; // unit vector
    vec3 rayBoxCorner; // Either (0,0,0) for nearest neighbor, or (0.5,0.5,0.5) for trilinear/tricubic
    vec3 forwardMask; // elements are zero or one, depending on view direction
    vec3 textureScale; // for converting texels to normalized texture coordinates
};

struct ViewSlab
{
    float minRayParam;
    float maxRayParam;
};

// Where does the ray penetrate the current voxel, in ray parameter units?
struct VoxelRayState
{
    float entryRayParameter; // where the ray enters the current voxel
    float middleRayParameter; // centroid of ray segment in the current voxel
    float exitRayParameter; // where the ray exists the current voxel
};

// Measured color values at various points within a voxel
struct VoxelIntensity
{
    COLOR_VEC voxelEntryIntensity; // color where ray enters voxel
    COLOR_VEC voxelMiddleIntensity; // color at subvoxel ray centroid
    COLOR_VEC voxelExitIntensity; // color where ray exits voxel
    float latestRayParameter;
};


// Delegated Methods for compartmentalized volume rendering algorithm

float advance_to_voxel_edge(in float previousEdge, in RayParameters rayParameters) 
{
    // Units of ray parameter, t, are roughly texels
    const float minStep = 0.01;

    // Advance ray by at least minStep, to avoid getting stuck in tiny corners
    float t = previousEdge + minStep;
    vec3 x0 = rayParameters.rayOriginInTexels;
    vec3 x1 = rayParameters.rayDirectionInTexels; 
    vec3 currentTexelPos = (x0 + t*x1); // apply ray equation to find new voxel

    // Advance ray to next voxel edge.
    // For NEAREST filter, advance to midplanes between voxel centers.
    // For TRILINEAR and TRICUBIC filters, advance to planes connecing voxel centers.
    vec3 currentTexel = floor(currentTexelPos + rayParameters.rayBoxCorner) 
            - rayParameters.rayBoxCorner;

    // Three out of six total voxel edges represent forward progress
    vec3 forwardMask = rayParameters.forwardMask;
    vec3 candidateEdges = currentTexel + forwardMask;
    // Ray trace to three planar voxel edges at once.
    vec3 candidateSteps = -(x0 - candidateEdges)/x1;
    // Choose the closest voxel edge.
    float nextEdge = min(candidateSteps.x, min(candidateSteps.y, candidateSteps.z));
    // Advance ray by at least minStep, to avoid getting stuck in tiny corners
    // Next line should be unneccessary, but prevents (sporadic?) driver crash
    nextEdge = max(nextEdge, previousEdge + minStep);
    return nextEdge;
}

VoxelRayState find_first_voxel(in RayBounds rayBounds, in RayParameters rayParameters) {
    float t1 = rayBounds.minRayParameter;
    float t3 = advance_to_voxel_edge(t1, rayParameters);
    float t2 = (t1 + t3)/2.0;
    return VoxelRayState(t1, t2, t3);
}

// Compute begin and end points of ray, once and for all
RayBounds initialize_ray_bounds(in RayParameters rayParameters, in ViewSlab viewSlab) 
{
    // bound ray within view slab thickness
    float slabTMin = viewSlab.minRayParam;
    float slabTMax = viewSlab.maxRayParam;

    float tMin = slabTMin;
    float tMax = slabTMax;

    // Bound ray within texture coordinate range (0,1)
    // TODO: Allow per-brick restrictions to even smaller TC subsets
    vec3 texelMax = rayParameters.forwardMask / rayParameters.textureScale;
    vec3 reverseMask = vec3(1) - rayParameters.forwardMask;
    vec3 texelMin = reverseMask / rayParameters.textureScale;
    vec3 x0 = rayParameters.rayOriginInTexels;
    vec3 x1 = rayParameters.rayDirectionInTexels;
    // Compute parameter t limits in all three directions at once, 
    // using plane ray tracing equation.
    vec3 vtMin = -(x0 - texelMin)/x1;
    float texCoordTMin = max(max(vtMin.x, vtMin.y), vtMin.z); // looks OK
    vec3 vtMax = -(x0 - texelMax)/x1;
    float texCoordTMax = min(min(vtMax.x, vtMax.y), vtMax.z); // OK?

    tMin = max(tMin, texCoordTMin);
    tMax = min(tMax, texCoordTMax);

    // Clip by depth buffer from already-rendered opaque objects, such as neuron models
    // http://web.archive.org/web/20130416194336/http://olivers.posterous.com/linear-depth-in-glsl-for-real
    // next line assumes that opaqueDepthTexture is the same size as the current viewport
    vec2 depthTc = gl_FragCoord.xy / textureSize(opaqueDepthTexture, 0); // compute texture coordinate for depth lookup
    float z_buf = texture(opaqueDepthTexture, depthTc).x; // raw depth value from z-buffer
    float zNear = opaqueZNearFar.x;
    float zFar = opaqueZNearFar.y;
    float z_eye = 2*zFar*zNear / (zFar + zNear - (zFar - zNear)*(2*z_buf - 1));
    vec4 depth_plane_eye = vec4(0, 0, 1, z_eye);
    vec4 depth_plane_tc = transpose(tcToCamera)*depth_plane_eye; // it's complicated...
    vec4 depth_plane_texels = vec4(depth_plane_tc.xyz*rayParameters.textureScale, depth_plane_tc.w);
    float tDepth = -dot(depth_plane_texels, vec4(x0,1)) / dot(depth_plane_texels, vec4(x1,0));
    // z_buf tends to be zero when depth texture is uninitialized
    // I hope valid zero values are uncommon...
    // z_buf should be 1.0 where there was no opaque geometry, so skip those too.
    if ((z_buf != 0) && (z_buf < 0.9999)) {
        tMax = min(tDepth, tMax); // Don't cast ray past that opaque object
    }

    return RayBounds(tMin, tMax);
}

RayParameters initialize_ray_parameters() {
    ivec3 texelsPerVolume = textureSize(volumeTexture, levelOfDetail);

    vec3 originInTexels = fragTexCoord * texelsPerVolume; // on near face of volume geometry
    vec3 directionInTexels = normalize( (fragTexCoord - camPosInTc) * texelsPerVolume );

    vec3 rayBoxCorner; // Body-centered vs corner-centered voxels
    if (filteringOrder == 0)  // nearest neighbor
    {
        rayBoxCorner = vec3(0, 0, 0); // intersect ray at voxel edge planes, optimal for nearest-neighbor
    } 
    else  // trilinear and tricubic
    {
        rayBoxCorner = 
                vec3(0.5, 0.5, 0.5); // intersect ray at voxel center planes
    }

    vec3 forwardMask = ceil(directionInTexels * 0.99); // each component is now 0 or 1

    vec3 textureScale = vec3(1,1,1) / texelsPerVolume;

    return RayParameters(originInTexels, directionInTexels, rayBoxCorner, forwardMask, textureScale);
}

ViewSlab initialize_view_slab(RayParameters rayParams) 
{
    vec3 x0 = rayParams.rayOriginInTexels;
    vec3 x1 = rayParams.rayDirectionInTexels;
    // Problem: planes are in texture coordinates, ray parameters are in texels
    // Solution: Convert plane equation to work with texel value based ray parameters, instead of with texture coordinate based ray parameters
    vec4 nearSlabTexels = vec4(nearSlabPlane.xyz*rayParams.textureScale, nearSlabPlane.w);
    vec4 farSlabTexels = vec4(farSlabPlane.xyz*rayParams.textureScale, farSlabPlane.w);
    float tMinSlab = -dot(nearSlabTexels, vec4(x0,1)) / dot(nearSlabTexels, vec4(x1,0));
    float tMaxSlab = -dot(farSlabTexels, vec4(x0,1)) / dot(farSlabTexels, vec4(x1,0));
    return ViewSlab(tMinSlab, tMaxSlab);
}

// Integrate the latest sample into the accumulated color
void integrate_intensity(
        in VoxelIntensity localIntensity, 
        inout IntegratedIntensity integratedIntensity,
        inout CoreStatus core,
        in ViewSlab viewSlab,
        in VoxelRayState voxelRayState,
        in RayParameters rayParams)
{
    // Sample the correct part of the voxel
    COLOR_VEC vecLocalIntensity;
    if (filteringOrder == FILTER_NEAREST) {
        vecLocalIntensity = localIntensity.voxelMiddleIntensity;
    } else {
        // NOTE: We are using one sample per voxel for now. For better/maximum accuracy we could be using 2 or 3
        vecLocalIntensity = localIntensity.voxelExitIntensity;
    }

    float rayParameter = localIntensity.latestRayParameter;

    // Also track the location of the tracing channel core,
    // using a sort-of MIP approach, regardless of rendering projection
    // TODO: generalize tracing channel for unmixing, as an adjustable input per-block
    const COLOR_VEC tracingChannel = COLOR_VEC(1, 0); // We only care about seeking the core of the channel used for tracing
    float coreIntensity = dot(vecLocalIntensity, tracingChannel);

    if (coreIntensity > integratedIntensity.coreIntensity) { // new brightest core, definitely worth keeping
        integratedIntensity.coreIntensity = coreIntensity;
        core.inLocalBody = true;
        core.firstBodyRayParam = core.finalBodyRayParam = rayParameter;
        integratedIntensity.coreRayParameter = rayParameter;
    }
    else if (core.inLocalBody) { // maybe continue core we saw earlier
        if (coreIntensity == integratedIntensity.coreIntensity) { // cruise intensity plateau
            core.finalBodyRayParam = rayParameter; // record extension of plateau
            integratedIntensity.coreRayParameter = mix(core.firstBodyRayParam, core.finalBodyRayParam, 0.5);
        }
        else { // dimmer intensity means we are past the core
            core.inLocalBody = false;
        }
    }

    // Isosurface mode considered first, so we can skip fade calculation (below)
    #if PROJECTION_MODE == PROJECTION_ISOSURFACE
        if (integratedIntensity.opacity > 0.5)
            return; // presumably already hit the threshold earlier in the ray
        COLOR_VEC isoThreshold = 0.5 * (opacityFunctionMin + opacityFunctionMax);
        float threshDist = maxElement(vecLocalIntensity - isoThreshold);
        if (threshDist <= 0) // surface not intersected
            return;
        // If we get this far, we just now penetrated the isosurface threshold
        integratedIntensity.opacity = 1.0; // opaque surface
        // Where, precisely, did the ray penetrate the surface?
        float surfaceRayParam;
        if (filteringOrder == FILTER_NEAREST) {
            surfaceRayParam = voxelRayState.entryRayParameter; // retreat to voxel surface
        }
        else {
            // Interpolate between previous ray spot, and current ray spot.
            COLOR_VEC previousIntensity = localIntensity.voxelEntryIntensity;
            float previousThreshDist = maxElement(previousIntensity - isoThreshold);
            float alpha = (-previousThreshDist) / (threshDist - previousThreshDist);
            alpha = clamp(alpha, 0, 1);
            surfaceRayParam = mix(voxelRayState.entryRayParameter, voxelRayState.exitRayParameter, alpha);
        }
        vec3 x0 = rayParams.rayOriginInTexels;
        vec3 x1 = rayParams.rayDirectionInTexels;
        vec3 surfaceTexel = x0 + surfaceRayParam * x1;
        // Use density gradient as surface normal
        vec3 voxelMicrometers = volumeMicrometers * rayParams.textureScale;
        vec3 normal = calculateNormalInScreenSpace(
            volumeTexture,
            surfaceTexel,
            voxelMicrometers,
            rayParams.textureScale);
        normal = 0.5 * (normal + vec3(1, 1, 1)); // restrict to range 0-1
        integratedIntensity.intensity = COLOR_VEC(normal); // Maybe just X/Y components, if vec2
        return; // stop casting! we found the surface along this ray
    #endif // PROJECTION ISOSURFACE

    // compute scalar proxy for intensity
    float localOpacity = maxElement(rampstep(opacityFunctionMin, opacityFunctionMax, vecLocalIntensity));

    // shortcut - skip sufficiently dim intensities
    if (localOpacity <= 0)
        return;

    // fade intensity at front and back, for smoother clipping
    // NOTE - this fading effect is nice because it avoids popping when objects
    // enter and leave the view slab.
    // BUT the fading effect has performance consequences:
    //  * obviously it adds computations to the slow loop
    //  * ...including the early computation of relative depth
    //  * it also uses the OpacityFunction, which is otherwise not strictly
    //    needed in this shader at all. So, for example, without the fade
    //    effect, this entire render pass could be cached and skipped, as
    //    the user drags the opacity parameters. 
    float rd = (rayParameter - viewSlab.minRayParam) 
            / (viewSlab.maxRayParam - viewSlab.minRayParam); // relative depth
    const float fadeBuffer = 0.20; // how much of depth slab to include in fading
    float fade = rampstep(0.0, fadeBuffer, rd);
    fade = min(fade, rampstep(1.0, 1.0 - fadeBuffer, rd));
    localOpacity *= fade;

    // Integrate
    #if PROJECTION_MODE == PROJECTION_MAXIMUM
        if (localOpacity > integratedIntensity.opacity) {
            integratedIntensity.intensity = vecLocalIntensity;
            integratedIntensity.opacity = localOpacity;
        }
    #elif PROJECTION_MODE == PROJECTION_OCCLUDING
        COLOR_VEC c_src = vecLocalIntensity;
        float a_src = localOpacity;

        // Previous integrated values are in FRONT of new values
        COLOR_VEC c_dest = integratedIntensity.intensity;
        float a_dest = integratedIntensity.opacity;

        float a_out = 1.0 - (1.0 - a_src)*(1.0 - a_dest);
        COLOR_VEC c_out = c_dest*a_dest/a_out + c_src*(1.0 - a_dest/a_out);

        integratedIntensity.intensity = clamp(c_out, 0, 1); // clamp required to avoid black artifacts
        integratedIntensity.opacity = a_out;
    #else // isosurface
        // NOTE: isosurface projection already handled earlier
    #endif // PROJECTION_MODE

}

// Are we done casting the current view ray?
bool ray_complete(
        in VoxelRayState state, 
        in RayBounds bounds, 
        in IntegratedIntensity intensity,
        in CoreStatus core)
{
    if (state.exitRayParameter >= bounds.maxRayParameter) 
        return true; // exited back of block
    if ((intensity.opacity >= 0.99) && (! core.inLocalBody)) 
        return true; // stop at full occlusion
    return false;
}

// Fetch the color of the current voxel
// AND track brightest point in tracing channel
void sample_intensity(
        in VoxelRayState rayState, 
        in RayParameters rayParams, 
        inout VoxelIntensity intensity) 
{
    // Copy exit edge of previous voxel, to entry edge of current voxel
    intensity.voxelEntryIntensity = intensity.voxelExitIntensity;

    // Compute ray parameters
    float t; // ray parameter at sample location
    if (filteringOrder == FILTER_NEAREST) {
        t = rayState.middleRayParameter; // Sample at center for nearest neighbor
    } else {
        t = rayState.exitRayParameter;
    }
    vec3 texelPos = rayParams.rayOriginInTexels + t * rayParams.rayDirectionInTexels;
    vec3 textureScale = rayParams.textureScale;

    COLOR_VEC vecLocalIntensity;
    if (filteringOrder == FILTER_TRICUBIC) {
        // slow tricubic filtering
        vecLocalIntensity = filterFastCubic3D(volumeTexture, texelPos, textureScale, levelOfDetail);
    }
    else {
        // fast linear or nearest-neighbor filtering
        vec3 texCoord = texelPos * textureScale;
        vecLocalIntensity = COLOR_VEC(textureLod(volumeTexture, texCoord, levelOfDetail));
    }

    // Save fetched intensity
    if (filteringOrder == FILTER_NEAREST) {
        intensity.voxelMiddleIntensity = vecLocalIntensity;
    } else {
        intensity.voxelExitIntensity = vecLocalIntensity;
    }

    intensity.latestRayParameter = t;
}

// Write out the final color/data after volume ray casting
void save_color(in IntegratedIntensity i, in ViewSlab slab) 
{
    colorOut = vec4(i.intensity, i.coreIntensity, i.opacity);
    float relativeDepth = (i.coreRayParameter - slab.minRayParam) / (slab.maxRayParam - slab.minRayParam);
    pickId = ivec2(3, int(relativeDepth * 65535));
}

// Advance ray by one voxel
void step_ray(in RayParameters rayParams, inout VoxelRayState voxel) {
    float t0 = voxel.exitRayParameter; // old leading edge
    float t1 = advance_to_voxel_edge(t0, rayParams);
    voxel.entryRayParameter = t0; // new trailing edge = old leading edge
    voxel.exitRayParameter = t1; // new leading edge
    voxel.middleRayParameter = mix(t0, t1, 0.5);
}

// Use volume ray casting to compute the color/data for one pixel on the screen
IntegratedIntensity cast_volume_ray(in RayParameters rayParameters, in ViewSlab viewSlab)
{
    // 1) Initialize beginning of ray
    RayBounds rayBounds = initialize_ray_bounds(rayParameters, viewSlab);
    VoxelRayState voxelRayState = find_first_voxel(rayBounds, rayParameters);
    IntegratedIntensity integratedIntensity = IntegratedIntensity(COLOR_VEC(0), 0, 0, 0);
    COLOR_VEC black = COLOR_VEC(0);
    VoxelIntensity voxelIntensity = VoxelIntensity(black, black, black, 0);
    CoreStatus coreStatus = CoreStatus(false, 0, 0); // For finding neurite centroid
    // 2) March along the ray, one voxel at a time
    int stepCount = 0;
    const int maxStepCount = 200; // protect against infinite loops or oversize volumes
    while(true) {
        if (ray_complete(voxelRayState, rayBounds, integratedIntensity, coreStatus))
            return integratedIntensity; // DONE, we made it to the far side of the volume
        sample_intensity(voxelRayState, rayParameters, voxelIntensity); // this is an expensive step, due to texture fetch(es)
        integrate_intensity(
                voxelIntensity, 
                integratedIntensity, 
                coreStatus, viewSlab, voxelRayState, rayParameters);
        step_ray(rayParameters, voxelRayState);
        stepCount += 1;
        if (stepCount >= maxStepCount) // terminate early to avoid performance problems and sidestep inifinite-loop bugs
            return integratedIntensity;
    }
    return integratedIntensity; // function probably never gets this far
}

void main() {
    RayParameters rayParams = initialize_ray_parameters();
    ViewSlab viewSlab = initialize_view_slab(rayParams);
    IntegratedIntensity integratedIntensity = cast_volume_ray(rayParams, viewSlab);
    if (integratedIntensity.opacity <= 0.005) 
        discard; // This ray is invisible, so try to save resources by not painting it.
    save_color(integratedIntensity, viewSlab);
}
