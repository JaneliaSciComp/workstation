#version 430 core

/**
 * Tetrahdedral volume rendering fragment shader.
 */

/* 
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

// Optimize for a certain maximum number of color channels
// Input image channels
#define CHANNEL_VEC vec2
// Total output channels, including synthetic channels constructed here, in the fragment shader
#define OUTPUT_CHANNEL_VEC vec3

// three-dimensional raster volume of intensities through which we will cast view rays
layout(binding = 0) uniform sampler3D volumeTexture;

// palette used to compose "hot" color transfer functions from hue/saturation bases
layout(binding = 1) uniform sampler2D colorMapTexture;

// per-channel intensity transfer function
layout(location = 3) uniform OUTPUT_CHANNEL_VEC opacityFunctionMin = OUTPUT_CHANNEL_VEC(0);
layout(location = 4) uniform OUTPUT_CHANNEL_VEC opacityFunctionMax = OUTPUT_CHANNEL_VEC(1);
layout(location = 5) uniform OUTPUT_CHANNEL_VEC opacityFunctionGamma = OUTPUT_CHANNEL_VEC(1);

layout(location = 6) uniform OUTPUT_CHANNEL_VEC channelVisibilityMask = OUTPUT_CHANNEL_VEC(1);

// use a linear combination of input color channels to create one channel used for neuron tracing
// used for computing "core" depth and intensity
// Parameters for channel unmixing
layout(location = 7) uniform vec4 unmixMinScale = vec4(0.0, 0.0, 0.5, 0.5);

// Parameters for reconstruction of original 16-bit channel intensities
layout(location = 8) uniform CHANNEL_VEC channelIntensityGamma = CHANNEL_VEC(1);
layout(location = 9) uniform CHANNEL_VEC channelIntensityScale = CHANNEL_VEC(1);
layout(location = 10) uniform CHANNEL_VEC channelIntensityOffset = CHANNEL_VEC(0);

// Channel colors
layout(location = 11) uniform OUTPUT_CHANNEL_VEC channelColorHue = OUTPUT_CHANNEL_VEC(120, 300, 210);
layout(location = 12) uniform OUTPUT_CHANNEL_VEC channelColorSaturation = OUTPUT_CHANNEL_VEC(1);

#define PROJECTION_MAXIMUM 0
#define PROJECTION_OCCLUDING 1
layout(location = 13) uniform int projectionMode = PROJECTION_MAXIMUM;

// TODO: Focus distance

in vec3 fragTexCoord; // texture coordinate at back face of tetrahedron
flat in vec3 cameraPosInTexCoord; // texture coordinate at view eye location
flat in mat4 tetPlanesInTexCoord; // clip plane equations at all 4 faces of tetrhedron
flat in vec4 zNearPlaneInTexCoord; // clip plane equation at near view slab plane
flat in vec4 zFarPlaneInTexCoord; // plane equation for far z-clip plane
flat in vec4 zFocusPlaneInTexCoord; // plane equation for far z-clip plane

layout(location = 0) out vec4 fragColor; // store final output color in the usual way
layout(location = 1) out vec2 coreDepth; // also store intensity and relative depth of the most prominent point along the ray, in a secondary render target


float max_element(in vec2 v) {
    return max(v.r, v.g);
}

vec2 rampstep(vec2 edge0, vec2 edge1, vec2 x) {
    return clamp((x - edge0)/(edge1 - edge0), 0.0, 1.0);
}

vec3 rampstep(vec3 edge0, vec3 edge1, vec3 x) {
    return clamp((x - edge0)/(edge1 - edge0), 0.0, 1.0);
}

// Unmixes one voxel from two channels to create a third, synthetic channel voxel
float tracing_channel_from_measured(CHANNEL_VEC raw_channels) {
    vec2 raw = raw_channels.xy;
    // Avoid extreme differences at low input intensity
    if (raw.x < 0.99 * unmixMinScale.x) return 0; // below threshold -> no data
    if (raw.y < 0.99 * unmixMinScale.y) return 0;
    // scale the two channels and combine
    float result = dot(raw_channels.xy, unmixMinScale.zw);
    // adjust the minimum to roughly match one of the input channels
    float offset = -dot(unmixMinScale.xy, unmixMinScale.zw); // move average black level to zero
    // restore black level to match one of the inputs
    if (unmixMinScale.z >= unmixMinScale.w) offset += unmixMinScale.x; // use black level from channel 1
    else offset += unmixMinScale.y; // use black level from channel 2
    result += offset; // allow room to explore negative differences
    result = clamp(result, 0, 1);
    return result;
}

// Opacity values must be between zero and one
float opacity_from_rescaled_channels(in vec3 intensities) {
    const vec3 ones = vec3(1);
    vec3 t = ones - intensities; // transparency
    return clamp(1.0 - t.r * t.g * t.b, 0, 1);
}

OUTPUT_CHANNEL_VEC rescale_intensities(in OUTPUT_CHANNEL_VEC intensity) {
    OUTPUT_CHANNEL_VEC result = OUTPUT_CHANNEL_VEC(intensity);
    result -= opacityFunctionMin;
    result /= (opacityFunctionMax - opacityFunctionMin);
    result *= channelVisibilityMask;
    result = clamp(result, 0, 1);
    result = pow(result, opacityFunctionGamma);
    return result;
}

vec3 hot_color_for_hue_intensity(in float hue, in float saturation, in float intensity) {
    // hue
    float h = fract(2.0 + hue / 360.0); // normalize 360 degrees to range 0.0-1.0
    float s_sat = (0.7500 * h + 0.1875); // restrict to rainbow region of color map
    const float s_gray = 0.0625; // location of grayscale stripe
    // intensity
    float i = clamp(intensity, 0, 1);
    // i = pow(i, 1.0); // crude gamma correction of sRGB texture
    float r = (0.93750 * i + 0.03125); // dark to light, terminating at pixel centers
    vec3 color_sat = texture(colorMapTexture, vec2(r, s_sat)).rgb;
    vec3 color_gray = texture(colorMapTexture, vec2(r, s_gray)).rgb;
    return mix(color_gray, color_sat, saturation);
}

vec3 cold_color_for_hue_intensity(in float hue, in float saturation, in float intensity) {
    return hot_color_for_hue_intensity(hue, saturation, 1.0 - intensity);
}

vec4 rgba_for_scaled_intensities(in vec3 rescaled) {
    // hot color map
    vec3 ch1 = hot_color_for_hue_intensity(channelColorHue.r, channelColorSaturation.r, rescaled.r); // green
    vec3 ch2 = hot_color_for_hue_intensity(channelColorHue.g, channelColorSaturation.g, rescaled.g); // magenta
    vec3 ch3 = hot_color_for_hue_intensity(channelColorHue.b, channelColorSaturation.b, rescaled.b); // aqua blue
    const vec3 ones = vec3(1);
    vec3 combined = ones - (ones - ch1)*(ones - ch2)*(ones - ch3); // compromise between sum and max
    combined = clamp(combined, 0, 1);
    float opacity = opacity_from_rescaled_channels(rescaled);
    return vec4(combined, opacity);
}

float intersectRayAndPlane(
        in vec3 rayStart, in vec3 rayDirection, 
        in vec4 plane)
{
    float intersection = -(dot(rayStart, plane.xyz) + plane.w) / dot(plane.xyz, rayDirection);
    return intersection;
}

// Return ray parameter where ray intersects plane
void clipRayToPlane(
        in vec3 rayStart, in vec3 rayDirection, 
        in vec4 plane,
        inout float begin, // current ray start parameter
        inout float end) // current ray end parameter
{
    float direction = dot(plane.xyz, rayDirection);
    if (direction == 0)
        return; // ray is parallel to plane
    float intersection = intersectRayAndPlane(rayStart, rayDirection, plane);
    if (direction > 0) // plane normal is along ray direction
        begin = max(begin, intersection);
    else // plane normal is opposite to ray direction
        end = min(end, intersection);
}

float advance_to_voxel_edge(
        in float previousEdge,
        in vec3 rayOriginInTexels,
        in vec3 rayDirectionInTexels,
        in vec3 rayBoxCorner,
        in vec3 forwardMask,
        in float texelsPerRay)
{
    // Units of ray parameter, t, are roughly texels
    const float minStep = 0.020 / texelsPerRay;

    // Advance ray by at least minStep, to avoid getting stuck in tiny corners
    float t = previousEdge + minStep;
    vec3 x0 = rayOriginInTexels;
    vec3 x1 = rayDirectionInTexels; 
    vec3 currentTexelPos = x0 + t*x1; // apply ray equation to find new voxel

    // Advance ray to next voxel edge.
    // For NEAREST filter, advance to midplanes between voxel centers.
    // For TRILINEAR and TRICUBIC filters, advance to planes connecing voxel centers.
    vec3 currentTexel = floor(currentTexelPos + rayBoxCorner) 
            - rayBoxCorner;

    // Three out of six total voxel edges represent forward progress
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

// Nearest-neighbor filtering
CHANNEL_VEC sample_nearest_neighbor(in vec3 texCoord, in int levelOfDetail)
{
    CHANNEL_VEC intensity = CHANNEL_VEC(textureLod(volumeTexture, texCoord, levelOfDetail));

    const bool reconstruct_intensity = true;
    if (reconstruct_intensity) {
        // Reconstruct original 16-bit intensity
        CHANNEL_VEC intensity2 = CHANNEL_VEC(intensity);
        intensity2 = pow(intensity2, channelIntensityGamma);
        intensity2 *= channelIntensityScale;
        intensity2 += channelIntensityOffset;
        if (intensity.x <= 0) intensity2.x = 0; // TODO: there has to be a neater way...
        if (intensity.y <= 0) intensity2.y = 0;
        if (intensity.x >= 1) intensity2.x = mix(intensity2.x, 1.0, 0.5); // TODO: there has to be a neater way...
        if (intensity.y >= 1) intensity2.y = mix(intensity2.x, 1.0, 0.5);
        intensity = intensity2;
    }

    return intensity;
}

// Maximum intensity projection
vec4 integrate_max_intensity(in vec4 front, in vec4 back) 
{
    return clamp(max(front, back), 0, 1);
}

// Occluding projection
vec4 integrate_occluding(in vec4 front, in vec4 back) 
{
    // Integrating front-to-back, so use the Under Operator
    float opacity = 1.0 - (1.0 - front.a)*(1.0 - back.a);
    float kFront = front.a / opacity;
    vec3 color = front.rgb * kFront + back.rgb * (1.0 - kFront);
    return clamp(vec4(color, opacity), 0, 1);
}

void main() 
{
    // Ray parameters
    vec3 x0 = cameraPosInTexCoord; // origin
    vec3 x1 = fragTexCoord - x0; // direction

    // Clip near and far ray bounds
    float minRay = 0; // eye position
    float maxRay = 1; // back face fragment location

    // Clip ray bounds to tetrahedral faces
    clipRayToPlane(x0, x1, tetPlanesInTexCoord[0], minRay, maxRay);
    clipRayToPlane(x0, x1, tetPlanesInTexCoord[1], minRay, maxRay);
    clipRayToPlane(x0, x1, tetPlanesInTexCoord[2], minRay, maxRay);
    clipRayToPlane(x0, x1, tetPlanesInTexCoord[3], minRay, maxRay);
    
    clipRayToPlane(x0, x1, zNearPlaneInTexCoord, minRay, maxRay);
    clipRayToPlane(x0, x1, zFarPlaneInTexCoord, minRay, maxRay);

    float slabMin = intersectRayAndPlane(x0, x1, zNearPlaneInTexCoord);
    float slabMax = intersectRayAndPlane(x0, x1, zFarPlaneInTexCoord);
    float tFocus = intersectRayAndPlane(x0, x1, zFocusPlaneInTexCoord);
    float standardPathLength = tFocus / 250.0;
    // Brighten up very thin slabs
    standardPathLength = min(standardPathLength, (slabMax - slabMin)/5.0);

    if (minRay > maxRay) discard; // draw nothing if ray is completely clipped away

    vec3 frontTexCoord = x0 + minRay * x1;
    vec3 rearTexCoord = x0 + maxRay * x1;

    // Set up for texel-by-texel ray marching
    const int levelOfDetail = 0; // TODO: adjust dynamically
    ivec3 texelsPerVolume = textureSize(volumeTexture, levelOfDetail);

    vec3 rayOriginInTexels = x0 * texelsPerVolume;
    vec3 rayDirectionInTexels = x1 * texelsPerVolume;
    float texelsPerRay = length(rayDirectionInTexels);
    const vec3 rayBoxCorner = vec3(0, 0, 0); // nearest neighbor
    vec3 forwardMask = ceil(normalize(rayDirectionInTexels) * 0.99); // each component is now 0 or 1

    vec3 frontTexel = frontTexCoord * texelsPerVolume;
    vec3 rearTexel = rearTexCoord * texelsPerVolume;

    // Cast ray through volume
    vec4 integratedColor = vec4(0);
    bool rayIsFinished = false;
    float t0 = minRay;
    float coreParam = mix(minRay, maxRay, 0.5);
    float coreIntensity = -1.0;
    for (int s = 0; s < 1000; ++s) 
    {
        float t1 = advance_to_voxel_edge(t0, 
                rayOriginInTexels, rayDirectionInTexels,
                rayBoxCorner, forwardMask, 
                texelsPerRay);
        if (t1 >= maxRay) {
            t1 = maxRay;
            rayIsFinished = true;
        }
        // float t = mix(t0, t1, 0.5);
        float t = mix(t0, t1, 0.50);
        vec3 texel = rayOriginInTexels + t * rayDirectionInTexels;
        vec3 texCoord = texel / texelsPerVolume;

        // Use levelOfDetail for intentional downsampling
        CHANNEL_VEC localIntensity = sample_nearest_neighbor(texCoord, levelOfDetail);

        float tracingIntensity = tracing_channel_from_measured(localIntensity);
        if (tracingIntensity >= coreIntensity) { // always use MIP criterion for secondary render target
            coreIntensity = tracingIntensity;
            coreParam = t;
        }

        OUTPUT_CHANNEL_VEC localCombined = OUTPUT_CHANNEL_VEC(localIntensity, tracingIntensity);
        OUTPUT_CHANNEL_VEC localRescaled = rescale_intensities(localCombined);
        vec4 localColor = rgba_for_scaled_intensities(localRescaled);

        if (projectionMode == PROJECTION_MAXIMUM)
            integratedColor = integrate_max_intensity(integratedColor, localColor);
        else { // PROJECTION_OCCLUDING
            // Use Beer-Lambert law to compute opacity
            float pathLength = (t1 - t0) / standardPathLength;
            float concentration = localColor.a;
            localColor.a = 1.0 - exp(-pathLength * concentration); // Longer path -> more opacity
            integratedColor = integrate_occluding(integratedColor, localColor);
        }

        // Terminate early if we hit an opaque surface
        if (integratedColor.a >= 0.999) { // 0.99 is too small
            rayIsFinished = true;
        }

        if (rayIsFinished)
            break;
        t0 = t1;
    }

    if (integratedColor.a < 0.005) { // Smaller values work better for MIP mode
        discard; // terminate early if there is nothing to show
    }

    // Secondary render target stores 16-bit core intensity, plus relative depth
    float relativeDepth = (coreParam - slabMin) / (slabMax - slabMin);
    // When rendering multiple blocks, we need to store a relative-depth value 
    // that could win a GL_MAX blend contest.
    //   1) pad the most significant bits with the opacity, so the most
    //      opaque ray segment wins. Not perfect, but should work pretty
    //      well in most sparse rendering contexts.
    //   2) reverse the sense of the relative depth, so in case of an
    //      opacity tie, the NEARER ray segment wins.
    // Use a floating point render target, because integer targets won't blend.
    // Pack the opacity into the first 7 bits of a 32-bit float mantissa
    uint opacityInt = clamp(uint(0x7f * integratedColor.a), 0, 0x7f); // 7 bits of opacity, range 0-127
    relativeDepth = 1.0 - relativeDepth; // In case of equal opacity, we want NEAR depths to beat FAR depths in a GL_MAX comparison
    // Keep depth strictly fractional, for unambiguous packing with integer opacity
    relativeDepth = clamp(relativeDepth, 0.0, 0.999);
    float opacityDepth = opacityInt + relativeDepth;
    coreIntensity = clamp(coreIntensity, 0, 1);
    coreDepth = vec2(coreIntensity, opacityDepth); // populates both channels of secondary render target

    // Primary render target stores final blended RGBA color
    fragColor = integratedColor;
}
