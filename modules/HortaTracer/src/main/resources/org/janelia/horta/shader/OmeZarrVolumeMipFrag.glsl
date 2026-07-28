#version 330

// Use #defines to compile separate shaders for each projection type
#define PROJECTION_MAXIMUM 0
#define PROJECTION_OCCLUDING 1
#define PROJECTION_ISOSURFACE 2
// REPLACE THE FOLLOWING LINE WITH CORRECT PROJECTION DEFINE
#define PROJECTION_MODE PROJECTION_MAXIMUM

/**
 * Multi-channel ray-casting volume shader for OME-Zarr.
 *
 * Forked from VolumeMipFrag.glsl. Where that shader packs (up to) two channels into a single
 * sampler3D, this one binds one single-component sampler3D per channel (channelTexture[c]) and
 * combines an arbitrary number of channels jointly in a single ray-march pass, mirroring the
 * channel-combination and in-shader unmixing of TetVolumeFrag330.glsl.
 *
 * GLSL 3.30 restriction: sampler arrays may only be indexed with a constant expression, so the
 * per-channel texture fetches are unrolled with literal indices (see FETCH_CHANNEL). Ordinary
 * float/vec uniform arrays (channelColor, channelMin, ...) may be indexed with a loop variable.
 */

// Must match MAX_CHANNELS in OmeZarrVolumeMipMaterial.java
#define MAX_CHANNELS 8

// primary render target: final blended RGBA color
layout(location = 0) out vec4 colorOut;
// secondary render target for picking / tracing: core intensity and packed opacity+depth
layout(location = 1) out vec2 pickId;

// One single-component volume texture per channel.
uniform sampler3D channelTexture[MAX_CHANNELS];
uniform int channelCount = 1;

// Per-channel transfer function, driven by the ImageColorModel (see setChannelUniforms()).
uniform vec3 channelColor[MAX_CHANNELS];   // display color of each channel
uniform float channelMin[MAX_CHANNELS];    // normalized black level
uniform float channelMax[MAX_CHANNELS];    // normalized white level
uniform float channelGamma[MAX_CHANNELS];  // gamma
uniform float channelVisible[MAX_CHANNELS]; // 1.0 visible, 0.0 hidden

// In-shader unmixing: the synthetic tracing channel is a signed linear combination of all channels,
// tracing = sum_c unmixScale[c] * (raw[c] - unmixMin[c]) + blackLevel. A negative scale subtracts a
// channel's bleed-through. Defaults (set from the legacy 2-channel params on the Java side) reproduce
// the original behavior: scale 0.5 on channels 0 and 1, zero elsewhere.
uniform float unmixMin[MAX_CHANNELS];
uniform float unmixScale[MAX_CHANNELS];

uniform vec3 camPosInTc; // camera position, in texture coordinate frame
uniform int levelOfDetail = 0; // volume texture LOD
uniform vec3 volumeMicrometers = vec3(256, 256, 200);
uniform float canonicalOccludingPathLengthUm = 1.0; // micrometers
uniform float fadeThicknessInTexels = 1000.0;

// Homogeneous clip plane equations, in texture coordinates
uniform vec4 nearSlabPlane;
uniform vec4 farSlabPlane;

// put surface normals in eye space for isosurface projection
uniform mat4 tcToCamera = mat4(1);

// clip using depth buffer from opaque pass
uniform sampler2D opaqueDepthTexture;
uniform vec2 opaqueZNearFar = vec2(1e-2, 1e4);

#define FILTER_NEAREST 0
#define FILTER_TRILINEAR 1
#define FILTER_TRICUBIC 3 // Expensive beautiful rendering option, for slow, high quality rendering passes
uniform int filteringOrder = 3; // 0: NEAREST; 1: TRILINEAR; 2: <not used> 3: TRICUBIC

in vec3 fragTexCoord; // texture coordinate at mesh surface of volume

// ---------------------------------------------------------------------------
// Tricubic filtering helpers (single component)
// ---------------------------------------------------------------------------

// Catmull-Rom spline actually passes through control points
vec4 cubic(float x)
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
// Adapted from https://groups.google.com/forum/#!topic/comp.graphics.api.opengl/kqrujgJfTxo
float filterFastCubic3D(sampler3D tex, vec3 texcoord, vec3 texscale, int lod)
{
    float fx = fract(texcoord.x);
    float fy = fract(texcoord.y);
    float fz = fract(texcoord.z);

    texcoord.x -= fx;
    texcoord.y -= fy;
    texcoord.z -= fz;

    vec4 xcubic = cubic(fx);
    vec4 ycubic = cubic(fy);
    vec4 zcubic = cubic(fz);

    vec3 c0 = texcoord - vec3(0.5, 0.5, 0.5);
    vec3 s0 = vec3(xcubic.x + xcubic.y, ycubic.x + ycubic.y, zcubic.x + zcubic.y);
    vec3 offset0 = c0 + vec3(xcubic.y, ycubic.y, zcubic.y) / s0;

    float sample000 = textureLod(tex, vec3(offset0.x, offset0.y, offset0.z) * texscale, lod).r;
    vec3 c1 = texcoord + vec3(1.5, 1.5, 1.5);
    vec3 s1 = vec3(xcubic.z + xcubic.w, ycubic.z + ycubic.w, zcubic.z + zcubic.w);
    vec3 offset1 = c1 + vec3(xcubic.w, ycubic.w, zcubic.w) / s1;
    float sample100 = textureLod(tex, vec3(offset1.x, offset0.y, offset0.z) * texscale, lod).r;
    float sx = s0.x / (s0.x + s1.x);
    float sampleX00 = mix(sample100, sample000, sx);
    float sample010 = textureLod(tex, vec3(offset0.x, offset1.y, offset0.z) * texscale, lod).r;
    float sample110 = textureLod(tex, vec3(offset1.x, offset1.y, offset0.z) * texscale, lod).r;
    float sy = s0.y / (s0.y + s1.y);
    float sampleX10 = mix(sample110, sample010, sx);
    float sampleXY0 = mix(sampleX10, sampleX00, sy);
    float sample001 = textureLod(tex, vec3(offset0.x, offset0.y, offset1.z) * texscale, lod).r;
    float sample101 = textureLod(tex, vec3(offset1.x, offset0.y, offset1.z) * texscale, lod).r;
    float sampleX01 = mix(sample101, sample001, sx);
    float sz = s0.z / (s0.z + s1.z);
    float sample011 = textureLod(tex, vec3(offset0.x, offset1.y, offset1.z) * texscale, lod).r;
    float sample111 = textureLod(tex, vec3(offset1.x, offset1.y, offset1.z) * texscale, lod).r;
    float sampleX11 = mix(sample111, sample011, sx);
    float sampleXY1 = mix(sampleX11, sampleX01, sy);

    return mix(sampleXY1, sampleXY0, sz);
}

float rampstep(float edge0, float edge1, float x) {
    return clamp((x - edge0)/(edge1 - edge0), 0.0, 1.0);
}

// ---------------------------------------------------------------------------
// Per-channel sampling and combination
// ---------------------------------------------------------------------------

// Fetch a single channel's raw normalized intensity at a texel position.
float sampleChannelRaw(sampler3D tex, vec3 texelPos, vec3 textureScale) {
    if (filteringOrder == FILTER_TRICUBIC)
        return filterFastCubic3D(tex, texelPos, textureScale, levelOfDetail);
    return textureLod(tex, texelPos * textureScale, levelOfDetail).r;
}

// Sample all active channels into raw[]. Sampler arrays require constant indices in GLSL 330,
// so this is unrolled; channels beyond channelCount are left at zero.
#define FETCH_CHANNEL(i) raw[i] = (i < channelCount) ? sampleChannelRaw(channelTexture[i], texelPos, textureScale) : 0.0;
void fetchChannels(vec3 texelPos, vec3 textureScale, out float raw[MAX_CHANNELS]) {
    FETCH_CHANNEL(0) FETCH_CHANNEL(1) FETCH_CHANNEL(2) FETCH_CHANNEL(3)
    FETCH_CHANNEL(4) FETCH_CHANNEL(5) FETCH_CHANNEL(6) FETCH_CHANNEL(7)
}

// Apply one channel's transfer function: black/white contrast stretch, gamma, visibility.
float rescaleChannel(int c, float raw) {
    float r = (raw - channelMin[c]) / max(channelMax[c] - channelMin[c], 1e-5);
    r = clamp(r, 0.0, 1.0);
    r = pow(r, channelGamma[c]);
    return r * channelVisible[c];
}

// Combine all channels into a display color (screen combine of each channel's color) and an
// aggregate opacity (1 - product of per-channel transparencies), mirroring TetVolumeFrag330.
// Also includes the synthetic tracing/unmix channel at index channelCount (see
// tracingChannelFromRaw()), so it gets its own color/contrast/visibility and can contribute to
// opacity on its own, exactly like a real channel -- mirrors the legacy TetVolumeActor behavior
// where the unmixed channel was jointly rescaled and combined with the real channels.
vec4 combineChannels(in float raw[MAX_CHANNELS]) {
    vec3 transparency = vec3(1.0);
    float opacityTransparency = 1.0;
    int totalChannels = min(channelCount + 1, MAX_CHANNELS);
    for (int c = 0; c < totalChannels; ++c) {
        float r = rescaleChannel(c, raw[c]);
        transparency *= (vec3(1.0) - clamp(r * channelColor[c], 0.0, 1.0));
        opacityTransparency *= (1.0 - r);
    }
    vec3 color = clamp(vec3(1.0) - transparency, 0.0, 1.0);
    float opacity = clamp(1.0 - opacityTransparency, 0.0, 1.0);
    return vec4(color, opacity);
}

// Synthesize the tracing channel as a signed linear combination of all measured channels.
float tracingChannelFromRaw(in float raw[MAX_CHANNELS]) {
    if (channelCount < 2)
        return raw[0];
    float result = 0.0;
    float minRebased = 1.0;
    int dominant = 0; // channel with the largest weight, whose black level we restore
    for (int c = 0; c < channelCount; ++c) {
        float rebased = raw[c] - unmixMin[c];
        minRebased = min(minRebased, rebased);
        result += unmixScale[c] * rebased;
        if (unmixScale[c] > unmixScale[dominant])
            dominant = c;
    }
    if (minRebased < -0.05)
        return 0.0; // below threshold -> no data
    result += unmixMin[dominant];
    return clamp(result, 0.0, 1.0);
}

// ---------------------------------------------------------------------------
// Isosurface normal (computed from channel 0 as a representative scalar field)
// ---------------------------------------------------------------------------

vec3 calculateNormalInScreenSpace(vec3 uvw, vec3 textureScale)
{
    const float delta = 0.75;
    const float downScale = 0.4;
    vec3 v0, v1;
    v0.x = downScale * sampleChannelRaw(channelTexture[0], uvw + vec3(delta, 0, 0), textureScale);
    v1.x = downScale * sampleChannelRaw(channelTexture[0], uvw - vec3(delta, 0, 0), textureScale);
    v0.y = downScale * sampleChannelRaw(channelTexture[0], uvw + vec3(0, delta, 0), textureScale);
    v1.y = downScale * sampleChannelRaw(channelTexture[0], uvw - vec3(0, delta, 0), textureScale);
    v0.z = downScale * sampleChannelRaw(channelTexture[0], uvw + vec3(0, 0, delta), textureScale);
    v1.z = downScale * sampleChannelRaw(channelTexture[0], uvw - vec3(0, 0, delta), textureScale);
    vec3 result = normalize(v1 - v0);
    result = (tcToCamera * vec4(result, 0)).xyz;
    return normalize(result);
}

// ---------------------------------------------------------------------------
// Ray-casting machinery (geometry only; unchanged from VolumeMipFrag.glsl,
// with the reference sampler being channelTexture[0])
// ---------------------------------------------------------------------------

// Accumulated values, for eventual display. Channels are integrated independently (element-wise
// max for MIP, per-channel under-operator for occluding) and only combined into a display color in
// save_color(), mirroring TetVolumeFrag330's integrate-then-colorize approach so that each channel's
// contribution survives the blend rather than being collapsed per voxel.
struct IntegratedIntensity
{
    float channel[MAX_CHANNELS]; // per-channel accumulated raw intensity (MIP/occluding)
    vec3 isoColor;               // surface color, isosurface mode only
    float opacity;
    float coreIntensity;    // brightest tracing-channel intensity yet seen
    float coreRayParameter; // location of coreIntensity
};

struct CoreStatus
{
    bool inLocalBody;
    float firstBodyRayParam;
    float finalBodyRayParam;
};

struct RayBounds { float minRayParameter; float maxRayParameter; };

struct RayParameters {
    vec3 rayOriginInTexels;
    vec3 rayDirectionInTexels;
    vec3 rayBoxCorner;
    vec3 forwardMask;
    vec3 textureScale;
};

struct ViewSlab { float minRayParam; float maxRayParam; };

struct VoxelRayState {
    float previousVoxelMiddleRayParameter;
    float entryRayParameter;
    float middleRayParameter;
    float exitRayParameter;
};

float advance_to_voxel_edge(in float previousEdge, in RayParameters rayParameters)
{
    const float minStep = 0.01;
    float t = previousEdge + minStep;
    vec3 x0 = rayParameters.rayOriginInTexels;
    vec3 x1 = rayParameters.rayDirectionInTexels;
    vec3 currentTexelPos = (x0 + t*x1);
    vec3 currentTexel = floor(currentTexelPos + rayParameters.rayBoxCorner)
            - rayParameters.rayBoxCorner;
    vec3 forwardMask = rayParameters.forwardMask;
    vec3 candidateEdges = currentTexel + forwardMask;
    vec3 candidateSteps = -(x0 - candidateEdges)/x1;
    float nextEdge = min(candidateSteps.x, min(candidateSteps.y, candidateSteps.z));
    nextEdge = max(nextEdge, previousEdge + minStep);
    return nextEdge;
}

VoxelRayState find_first_voxel(in RayBounds rayBounds, in RayParameters rayParameters) {
    float t1 = rayBounds.minRayParameter;
    float t3 = advance_to_voxel_edge(t1, rayParameters);
    float t2 = (t1 + t3)/2.0;
    return VoxelRayState(t1, t1, t2, t3);
}

RayBounds initialize_ray_bounds(in RayParameters rayParameters, in ViewSlab viewSlab)
{
    float tMin = viewSlab.minRayParam;
    float tMax = viewSlab.maxRayParam;

    vec3 texelMax = rayParameters.forwardMask / rayParameters.textureScale;
    vec3 reverseMask = vec3(1) - rayParameters.forwardMask;
    vec3 texelMin = reverseMask / rayParameters.textureScale;
    vec3 x0 = rayParameters.rayOriginInTexels;
    vec3 x1 = rayParameters.rayDirectionInTexels;
    vec3 vtMin = -(x0 - texelMin)/x1;
    float texCoordTMin = max(max(vtMin.x, vtMin.y), vtMin.z);
    vec3 vtMax = -(x0 - texelMax)/x1;
    float texCoordTMax = min(min(vtMax.x, vtMax.y), vtMax.z);

    tMin = max(tMin, texCoordTMin);
    tMax = min(tMax, texCoordTMax);

    // Clip by depth buffer from already-rendered opaque objects, such as neuron models
    vec2 depthTc = gl_FragCoord.xy / textureSize(opaqueDepthTexture, 0);
    float z_buf = texture(opaqueDepthTexture, depthTc).x;
    float zNear = opaqueZNearFar.x;
    float zFar = opaqueZNearFar.y;
    float z_eye = 2*zFar*zNear / (zFar + zNear - (zFar - zNear)*(2*z_buf - 1));
    vec4 depth_plane_eye = vec4(0, 0, 1, z_eye);
    vec4 depth_plane_tc = transpose(tcToCamera)*depth_plane_eye;
    vec4 depth_plane_texels = vec4(depth_plane_tc.xyz*rayParameters.textureScale, depth_plane_tc.w);
    float tDepth = -dot(depth_plane_texels, vec4(x0,1)) / dot(depth_plane_texels, vec4(x1,0));
    if ((z_buf != 0) && (z_buf < 0.9999)) {
        tMax = min(tDepth, tMax);
    }

    return RayBounds(tMin, tMax);
}

RayParameters initialize_ray_parameters() {
    ivec3 texelsPerVolume = textureSize(channelTexture[0], levelOfDetail);

    vec3 originInTexels = fragTexCoord * texelsPerVolume;
    vec3 directionInTexels = normalize( (fragTexCoord - camPosInTc) * texelsPerVolume );

    vec3 rayBoxCorner;
    if (filteringOrder == 0)
        rayBoxCorner = vec3(0, 0, 0);
    else
        rayBoxCorner = vec3(0.5, 0.5, 0.5);

    vec3 forwardMask = ceil(directionInTexels * 0.99);
    vec3 textureScale = vec3(1,1,1) / texelsPerVolume;

    return RayParameters(originInTexels, directionInTexels, rayBoxCorner, forwardMask, textureScale);
}

ViewSlab initialize_view_slab(RayParameters rayParams)
{
    vec3 x0 = rayParams.rayOriginInTexels;
    vec3 x1 = rayParams.rayDirectionInTexels;
    vec4 nearSlabTexels = vec4(nearSlabPlane.xyz*rayParams.textureScale, nearSlabPlane.w);
    vec4 farSlabTexels = vec4(farSlabPlane.xyz*rayParams.textureScale, farSlabPlane.w);
    float tMinSlab = -dot(nearSlabTexels, vec4(x0,1)) / dot(nearSlabTexels, vec4(x1,0));
    float tMaxSlab = -dot(farSlabTexels, vec4(x0,1)) / dot(farSlabTexels, vec4(x1,0));
    return ViewSlab(tMinSlab, tMaxSlab);
}

// Integrate the latest sample into the per-channel accumulator. Channels are kept separate here
// (combined into a color only in save_color) so that, for MIP, each channel's maximum along the ray
// survives independently rather than being collapsed to a single winning voxel.
void integrate_intensity(
        in float localRaw[MAX_CHANNELS], // raw per-channel intensity at this voxel
        in float localOpacity,           // aggregate opacity for this voxel (already faded)
        in float tracingIntensity,
        inout IntegratedIntensity integratedIntensity,
        inout CoreStatus core,
        in ViewSlab viewSlab,
        in VoxelRayState voxelRayState,
        in RayParameters rayParams)
{
    float rayParameter = voxelRayState.middleRayParameter;

    // Track the location of the tracing-channel core (MIP criterion, any projection mode)
    if (tracingIntensity > integratedIntensity.coreIntensity) {
        integratedIntensity.coreIntensity = tracingIntensity;
        core.inLocalBody = true;
        core.firstBodyRayParam = core.finalBodyRayParam = rayParameter;
        integratedIntensity.coreRayParameter = rayParameter;
    }
    else if (core.inLocalBody) {
        if (tracingIntensity == integratedIntensity.coreIntensity) {
            core.finalBodyRayParam = rayParameter;
            integratedIntensity.coreRayParameter = mix(core.firstBodyRayParam, core.finalBodyRayParam, 0.5);
        }
        else {
            core.inLocalBody = false;
        }
    }

    #if PROJECTION_MODE == PROJECTION_ISOSURFACE
        if (integratedIntensity.opacity > 0.5)
            return;
        const float isoThreshold = 0.5;
        if (localOpacity <= isoThreshold)
            return; // surface not intersected
        integratedIntensity.opacity = 1.0;
        float surfaceRayParam = voxelRayState.entryRayParameter;
        vec3 x0 = rayParams.rayOriginInTexels;
        vec3 x1 = rayParams.rayDirectionInTexels;
        vec3 surfaceTexel = x0 + surfaceRayParam * x1;
        vec3 normal = calculateNormalInScreenSpace(surfaceTexel, rayParams.textureScale);
        normal = 0.5 * (normal + vec3(1, 1, 1));
        integratedIntensity.isoColor = normal;
        return;
    #endif

    if (localOpacity <= 0)
        return;

    // fade intensity at front and back, for smoother clipping
    float rd = (rayParameter - viewSlab.minRayParam)
            / (viewSlab.maxRayParam - viewSlab.minRayParam);
    float fadeInterval = fadeThicknessInTexels / (viewSlab.maxRayParam - viewSlab.minRayParam);
    fadeInterval = min(0.40, fadeInterval);
    float fade = rampstep(0.0, fadeInterval, rd);
    fade = min(fade, rampstep(1.0, 1.0 - fadeInterval, rd));
    localOpacity *= fade;

    #if PROJECTION_MODE == PROJECTION_MAXIMUM
        // Maximum intensity projection: keep each channel's brightest value independently.
        // Includes the synthetic tracing channel at index channelCount (see cast_volume_ray()).
        int totalChannelsMax = min(channelCount + 1, MAX_CHANNELS);
        for (int c = 0; c < totalChannelsMax; ++c)
            integratedIntensity.channel[c] = max(integratedIntensity.channel[c], localRaw[c]);
        integratedIntensity.opacity = max(integratedIntensity.opacity, localOpacity);
    #elif PROJECTION_MODE == PROJECTION_OCCLUDING
        // Front-to-back "under" operator, applied per channel with a shared aggregate opacity.
        float a_src = localOpacity;
        float segmentLengthInRayParam = voxelRayState.exitRayParameter - voxelRayState.entryRayParameter;
        vec3 fineVoxelMicrometers = volumeMicrometers / textureSize(channelTexture[0], 0);
        vec3 voxelMicrometers = fineVoxelMicrometers;
        float umPerRayParam = dot(abs(rayParams.rayDirectionInTexels), voxelMicrometers);
        float segmentLengthInUm = segmentLengthInRayParam * umPerRayParam;
        float transmittance = 1.0 - a_src;
        float exponent = segmentLengthInUm / canonicalOccludingPathLengthUm;
        transmittance = pow(transmittance, exponent);
        a_src = 1.0 - transmittance;

        float a_dest = integratedIntensity.opacity; // already-integrated (in front) values
        float a_out = 1.0 - (1.0 - a_src)*(1.0 - a_dest);
        float kFront = (a_out > 0.0) ? a_dest / a_out : 0.0;
        // Includes the synthetic tracing channel at index channelCount (see cast_volume_ray()).
        int totalChannelsOcc = min(channelCount + 1, MAX_CHANNELS);
        for (int c = 0; c < totalChannelsOcc; ++c) {
            float blended = integratedIntensity.channel[c]*kFront + localRaw[c]*(1.0 - kFront);
            integratedIntensity.channel[c] = clamp(blended, 0.0, 1.0);
        }
        integratedIntensity.opacity = a_out;
    #endif
}

bool ray_complete(
        in VoxelRayState state,
        in RayBounds bounds,
        in IntegratedIntensity intensity,
        in CoreStatus core)
{
    if (state.exitRayParameter >= bounds.maxRayParameter)
        return true;
    if ((intensity.opacity >= 0.99) && (! core.inLocalBody))
        return true;
    return false;
}

void step_ray(in RayParameters rayParams, inout VoxelRayState voxel) {
    float t0 = voxel.exitRayParameter;
    float t1 = advance_to_voxel_edge(t0, rayParams);
    voxel.entryRayParameter = t0;
    voxel.exitRayParameter = t1;
    voxel.previousVoxelMiddleRayParameter = voxel.middleRayParameter;
    voxel.middleRayParameter = mix(t0, t1, 0.5);
}

IntegratedIntensity cast_volume_ray(in RayParameters rayParameters, in ViewSlab viewSlab)
{
    RayBounds rayBounds = initialize_ray_bounds(rayParameters, viewSlab);
    VoxelRayState voxelRayState = find_first_voxel(rayBounds, rayParameters);

    // Struct contains an array member, so it cannot use a constructor; initialize explicitly.
    IntegratedIntensity integratedIntensity;
    for (int c = 0; c < MAX_CHANNELS; ++c)
        integratedIntensity.channel[c] = 0.0;
    integratedIntensity.isoColor = vec3(0);
    integratedIntensity.opacity = 0.0;
    integratedIntensity.coreIntensity = 0.0;
    integratedIntensity.coreRayParameter = 0.0;

    CoreStatus coreStatus = CoreStatus(false, 0, 0);
    int stepCount = 0;
    const int maxStepCount = 800;
    while(true) {
        if (ray_complete(voxelRayState, rayBounds, integratedIntensity, coreStatus))
            return integratedIntensity;

        // Sample every channel at the voxel centroid
        float t = voxelRayState.middleRayParameter;
        vec3 texelPos = rayParameters.rayOriginInTexels + t * rayParameters.rayDirectionInTexels;
        float raw[MAX_CHANNELS];
        fetchChannels(texelPos, rayParameters.textureScale, raw);

        float tracingIntensity = tracingChannelFromRaw(raw);
        // Feed the synthetic tracing channel into the shared per-channel array (at the slot right
        // after the real channels) so it gets its own color/contrast/visibility and can
        // contribute to opacity, mirroring the legacy TetVolumeActor behavior.
        if (channelCount < MAX_CHANNELS)
            raw[channelCount] = tracingIntensity;

        float localOpacity = combineChannels(raw).a;

        integrate_intensity(
                raw,
                localOpacity,
                tracingIntensity,
                integratedIntensity,
                coreStatus, viewSlab, voxelRayState, rayParameters);

        step_ray(rayParameters, voxelRayState);
        stepCount += 1;
        if (stepCount >= maxStepCount)
            return integratedIntensity;
    }
    return integratedIntensity;
}

void save_color(in IntegratedIntensity i, in ViewSlab slab)
{
    #if PROJECTION_MODE == PROJECTION_ISOSURFACE
        vec3 color = i.isoColor;
    #else
        // Combine the per-channel accumulated intensities into a single display color now that
        // integration is complete (screen-combine of each channel's transfer-function color).
        vec3 color = combineChannels(i.channel).rgb;
    #endif
    colorOut = vec4(color, i.opacity);

    float relativeDepth = (i.coreRayParameter - slab.minRayParam) / (slab.maxRayParam - slab.minRayParam);
    relativeDepth = clamp(1.0 - relativeDepth, 0, 0.999);
    uint opacityInt = uint(clamp(int(i.opacity * 0x7f), 0, 0x7f));
    pickId = vec2(i.coreIntensity, opacityInt + relativeDepth);
}

void main() {
    RayParameters rayParams = initialize_ray_parameters();
    ViewSlab viewSlab = initialize_view_slab(rayParams);
    IntegratedIntensity integratedIntensity = cast_volume_ray(rayParams, viewSlab);
    if (integratedIntensity.opacity <= 0.005)
        discard;
    save_color(integratedIntensity, viewSlab);
}
