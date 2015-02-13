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

uniform mat4 tcToCamera = mat4(1);

// additional render target for picking
layout(location = 1) out ivec2 pickId;
uniform int pickIndex = 3; // default value for pick buffer

// Opacity component of transfer function
// NOTE: Ensure that these values are consistent with those from downstream contrast shader.
// (it might be OK for these to include a broader range; at least for MIP).
// TODO - avoid this hack I made to prevent default alpha==1.0 from causing problems
uniform vec4 opacityFunctionMin = vec4(0, 0, 0, 1);
uniform vec4 opacityFunctionMax = vec4(1, 1, 1, 2);

uniform vec3 camPosInTc; // camera position, in texture coordinate frame
uniform sampler3D volumeTexture; // the confocal image stack
uniform int levelOfDetail = 0; // volume texture LOD

// Use actual voxelSize, as uniform, for more accurate occlusion
uniform vec3 volumeMicrometers = vec3(256, 256, 200);
// TODO - expose occluding path length to user
uniform float canonicalOccludingPathLengthUm = 2.0; // micrometers

// Homogeneous clip plane equations, in texture coordinates
uniform vec4 nearSlabPlane; // for limiting view to a screen-parallel slab
uniform vec4 farSlabPlane; // for limiting view to a screen-parallel slab

// Expensive beautiful rendering option, for slow, high quality rendering passes
// TODO - set this dynamically depending on user interaction.
uniform int filteringOrder = 3; // 0: NEAREST; 1: TRILINEAR; 2: <not used> 3: TRICUBIC

// uniform int projectionMode = 0; // 0: maximum intensity; 1: occluding; 2: isosurface

in vec3 fragTexCoord; // texture coordinate at mesh surface of volume

// Helper method for tricubic filtering
// From http://stackoverflow.com/questions/13501081/efficient-bicubic-filtering-code-in-glsl
vec4 cubic(float x)
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

// Fast cubic interpolation using a source that is already linearly interpolated.
//   @param texture 3D texture to sample
//   @param texcoord 3D texture coordinate of point to sample, in non-normalized texel units
//   @param texscale 1/texelDimension of texture
//   @param lod level of detail mipmap texture level to use for sampling
// Adapted from https://groups.google.com/forum/#!topic/comp.graphics.api.opengl/kqrujgJfTxo
vec4 filterFastCubic3D(sampler3D texture, vec3 texcoord, vec3 texscale, int lod)
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
    vec3 c1 = texcoord + vec3(1.5, 1.5, 1.5);
    vec3 s0 = vec3(xcubic.x + xcubic.y, ycubic.x + ycubic.y, zcubic.x + zcubic.y);
    vec3 s1 = vec3(xcubic.z + xcubic.w, ycubic.z + ycubic.w, zcubic.z + zcubic.w);
    vec3 offset0 = c0 + vec3(xcubic.y, ycubic.y, zcubic.y) / s0;
    vec3 offset1 = c1 + vec3(xcubic.w, ycubic.w, zcubic.w) / s1;

    float sx = s0.x / (s0.x + s1.x);
    float sy = s0.y / (s0.y + s1.y);
    float sz = s0.z / (s0.z + s1.z);

    vec4 sample000 = textureLod(texture, vec3(offset0.x, offset0.y, offset0.z) * texscale, lod);
    vec4 sample100 = textureLod(texture, vec3(offset1.x, offset0.y, offset0.z) * texscale, lod);
    vec4 sampleX00 = mix(sample100, sample000, sx);
    vec4 sample010 = textureLod(texture, vec3(offset0.x, offset1.y, offset0.z) * texscale, lod);
    vec4 sample110 = textureLod(texture, vec3(offset1.x, offset1.y, offset0.z) * texscale, lod);
    vec4 sampleX10 = mix(sample110, sample010, sx);
    vec4 sampleXY0 = mix(sampleX10, sampleX00, sy);
    vec4 sample001 = textureLod(texture, vec3(offset0.x, offset0.y, offset1.z) * texscale, lod);
    vec4 sample101 = textureLod(texture, vec3(offset1.x, offset0.y, offset1.z) * texscale, lod);
    vec4 sampleX01 = mix(sample101, sample001, sx);
    vec4 sample011 = textureLod(texture, vec3(offset0.x, offset1.y, offset1.z) * texscale, lod);
    vec4 sample111 = textureLod(texture, vec3(offset1.x, offset1.y, offset1.z) * texscale, lod);
    vec4 sampleX11 = mix(sample111, sample011, sx);
    vec4 sampleXY1 = mix(sampleX11, sampleX01, sy);

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

vec4 rampstep(vec4 edge0, vec4 edge1, vec4 x) {
    return clamp((x - edge0)/(edge1 - edge0), 0.0, 1.0);
}

vec4 rampstepNoClamp(vec4 edge0, vec4 edge1, vec4 x) {
    return (x - edge0)/(edge1 - edge0);
}

vec3 rampstep(vec3 edge0, vec3 edge1, vec3 x) {
    return clamp((x - edge0)/(edge1 - edge0), 0.0, 1.0);
}

float maxElement(vec3 v) {
    return max( max(v.r, v.g), v.b);
}

float maxElement(vec4 v) {
    return max( max(v.r, v.g), max(v.b, v.a) );
}

float minElement(vec4 v) {
    return min( min(v.r, v.g), min(v.b, v.a) );
}

float sampleScaledIntensity(sampler3D volume, vec3 uvw, vec3 textureScale) 
{
    vec4 unscaled;
    if (filteringOrder == 3)
        unscaled = filterFastCubic3D(volume, uvw, textureScale, levelOfDetail);
    else
        unscaled = textureLod(volume, uvw*textureScale, levelOfDetail);
    return maxElement( unscaled.rgb );
}

vec3 calculateNormalInScreenSpace(sampler3D volume, vec3 uvw, vec3 voxelMicrometers, vec3 textureScale) 
{
    vec3 v[2];
    float delta = 0.75; // sample 1/2 pixel away

    // 0.2 *, to avoid overflow
    v[0].x = 0.4 * sampleScaledIntensity(volume, uvw + vec3(delta, 0, 0), textureScale);
    v[1].x = 0.4 * sampleScaledIntensity(volume, uvw - vec3(delta, 0, 0), textureScale);

    v[0].y = 0.4 * sampleScaledIntensity(volume, uvw + vec3(0, delta, 0), textureScale);
    v[1].y = 0.4 * sampleScaledIntensity(volume, uvw - vec3(0, delta, 0), textureScale);

    v[0].z = 0.4 * sampleScaledIntensity(volume, uvw + vec3(0, 0, delta), textureScale);
    v[1].z = 0.4 * sampleScaledIntensity(volume, uvw - vec3(0, 0, delta), textureScale);

    vec3 result = normalize( (v[1] - v[0]) );
    result = (tcToCamera * vec4(result, 0)).xyz; // TODO should be matrix that converts TC to camera
    return normalize(result);
}

void main() {
    vec4 vecIntegratedIntensity = vec4(0, 0, 0, 0); // up to 4 color channels
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
    float micrometersPerRay = dot(volumeMicrometers, abs(x1));

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
        // vec4 opacityFunctionMid = 0.5 * (opacityFunctionMin + opacityFunctionMax);
        vec4 isoThreshold = opacityFunctionMin;
        float previousThreshDist = 0;
        float previousT = previousEdge;
    #endif

    // Store results of ray casting at multiple positions:
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

    float maxDI = 0;

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
        previousEdge = nextEdge;
        previousDeltaT = deltaT;

        // shortcut
        if (segmentLength <= 0) continue;

        // Apply ray equation to compute texel coordinate from ray parameter.
        currentTexelPos = (x0 + t*x1); 
        vec3 texCoord = currentTexelPos * textureScale; // converted back to normalized texture coordinates,
        // Fetch texture intensity (EXPENSIVE!)
        vec4 vecLocalIntensity = vec4(0, 0, 0, 0);
        const bool doTriCubic = true;
        if (filteringOrder == 3) {
            // slow tricubic filtering
            vecLocalIntensity = filterFastCubic3D(volumeTexture, currentTexelPos, textureScale, levelOfDetail);
        }
        else {
            // fast linear or nearest-neighbor filtering
            vecLocalIntensity = textureLod(volumeTexture, texCoord, levelOfDetail);
        }

        // compute scalar proxy for intensity
        float localOpacity = maxElement(rampstep(opacityFunctionMin, opacityFunctionMax, vecLocalIntensity));

        // shortcut - skip sufficiently dim intensities
        if (localOpacity <= 0) continue;

        // for occluding projection, incorporation path length into opacity exponent
        #if PROJECTION_MODE == PROJECTION_OCCLUDING
            // Convert segmentLength to micrometers
            // segmentLength = micrometersPerRay * segmentLength;
            // float opacityExponent = canonicalOccludingPathLengthUm / segmentLength;
            // opacityExponent = 2.0;
            // localOpacity = pow(localOpacity, 2.0); // TODO - is this exponential slow?
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
                tMaxAbs = t;
            }
        #elif PROJECTION_MODE == PROJECTION_OCCLUDING
            // vec4 c_src = mix(opacityFunctionMin, vecLocalIntensity, fade);
            vec4 c_src = vecLocalIntensity;
            float a_src = localOpacity;

            // Previous integrated values are in FRONT of new values
            vec4 c_dest = vecIntegratedIntensity;
            float a_dest = integratedOpacity;

            float a_out = 1.0 - (1.0 - a_src)*(1.0 - a_dest);
            vec4 c_out = c_dest*a_dest/a_out + c_src*(1.0 - a_dest/a_out);
            // float a_out = a_dest + (1 - a_dest)*(a_src); // EQUIVALENT TO ABOVE
            // float c_out = c_dest + (1 - a_dest)*a_src*c_src;

            vecIntegratedIntensity = clamp(c_out, 0, 1); // clamp required to avoid black artifacts
            integratedOpacity = a_out;

            // Update brightest point along ray
            // TODO - click-to-center is not perfect yet...
            float dI = localOpacity * (1.0 - a_dest/a_out);
            if (dI > maxDI) {
                maxDI = dI;
                tMaxAbs = t;
            }
        #else // isosurface
            float threshDist = maxElement(vecLocalIntensity - isoThreshold) - 1e-6;
            if (threshDist > 0) // surface intersected
            {
                vecIntegratedIntensity = vecLocalIntensity;
                integratedOpacity = 1.0;
                // TODO - trying to interpolate ray parameter causes some thread pools to die
                float alpha = 0.5;
                // float alpha = (-previousThreshDist) / (threshDist - previousThreshDist);
                // if (isinf(alpha)) alpha = 1.0;
                // if (isnan(alpha)) alpha = 1.0;
                // alpha = clamp(alpha, 0, 1);
                // tMaxAbs = mix(previousT, t, alpha);
                // tMaxAbs = max(tMinMax.x, tMaxAbs);
                // tMaxAbs = min(tMaxAbs, t);
                tMaxAbs = mix(previousT, t, alpha);
                break;
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
    #if PROJECTION_MODE == PROJECTION_ISOSURFACE
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
    #else
    colorOut = vec4(vecIntegratedIntensity.xyz, integratedOpacity);
    #endif

    // pick value to alternate render target
    float relativeDepth = (tMaxAbs - tMinSlab) / (tMaxSlab - tMinSlab);
    pickId = ivec2(pickIndex, int(relativeDepth * 65535));

    // TODO gl_FragDepth for isosurface
}
