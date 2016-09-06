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
#define CHANNEL_VEC vec2

// three-dimensional raster volume of intensities through which we will cast view rays
layout(binding = 0) uniform sampler3D volumeTexture;
layout(binding = 1) uniform sampler2D colorMapTexture;

// Transfer function
layout(location = 3) uniform CHANNEL_VEC opacityFunctionMin = CHANNEL_VEC(0);
layout(location = 4) uniform CHANNEL_VEC opacityFunctionMax = CHANNEL_VEC(1);
layout(location = 5) uniform CHANNEL_VEC opacityFunctionGamma = CHANNEL_VEC(1);

layout(location = 6) uniform CHANNEL_VEC tracingChannelMask = CHANNEL_VEC(0.5);

in vec3 fragTexCoord;
flat in vec3 cameraPosInTexCoord;
flat in mat4 tetPlanesInTexCoord;
flat in vec4 zNearPlaneInTexCoord;
flat in vec4 zFarPlaneInTexCoord; // plane equation for far z-clip plane

// debugging only
in float fragZNear;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out ivec2 coreDepth;

struct IntegratedIntensity
{
    CHANNEL_VEC intensity;
    float opacity;
};

float max_element(in vec2 v) {
    return max(v.r, v.g);
}

vec2 rampstep(vec2 edge0, vec2 edge1, vec2 x) {
    return clamp((x - edge0)/(edge1 - edge0), 0.0, 1.0);
}

float opacity_for_intensities(in CHANNEL_VEC intensity) 
{
    // Use brightness model to modulate opacity
    CHANNEL_VEC rescaled = rampstep(opacityFunctionMin, opacityFunctionMax, intensity);
    rescaled = pow(rescaled, opacityFunctionGamma);
    // TODO: Is max across channels OK here? Or should we use something intermediate between MAX and SUM?
    return max_element(rescaled);
}

vec3 hot_color_for_hue_intensity(in float hue, in float intensity) {
    // hue
    float h = fract(2.0 + hue / 360.0); // normalize 360 degrees to range 0.0-1.0
    float s = (0.7500 * h + 0.1875); // restrict to rainbow region of color map
    // intensity
    float i = pow(intensity, 2.2); // crude gamma correction of sRGB texture
    float r = (0.93750 * i + 0.03125); // dark to light, terminating at pixel centers
    return texture(colorMapTexture, vec2(r, s)).rgb;
}

vec4 rgba_for_scaled_intensities(in vec2 c, in float opacity) {
    // return vec4(c.grg, opacity); // green/magenta

    // hot color map
    vec3 ch1 = hot_color_for_hue_intensity(120, c.r); // green
    vec3 ch2 = hot_color_for_hue_intensity(300, c.g); // magenta
    vec3 combined = ch1 + ch2 - ch1*ch2; // compromise between sum and max
    return vec4(combined, opacity);
}

vec4 rgba_for_intensities(IntegratedIntensity i) {
    // Use brightness model to modulate opacity
    CHANNEL_VEC rescaled = rampstep(opacityFunctionMin, opacityFunctionMax, i.intensity);
    rescaled = pow(rescaled, opacityFunctionGamma);
    return rgba_for_scaled_intensities(rescaled, i.opacity);
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
    const float minStep = 0.060 / texelsPerRay;

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
IntegratedIntensity sample_nearest_neighbor(in vec3 texCoord, in int levelOfDetail)
{
    CHANNEL_VEC intensity = CHANNEL_VEC(textureLod(volumeTexture, texCoord, levelOfDetail));
    float opacity = opacity_for_intensities(intensity);
    return IntegratedIntensity(intensity, opacity);
}

// Maximum intensity projection
IntegratedIntensity integrate_max_intensity(
        in IntegratedIntensity front, 
        in IntegratedIntensity back)
{
    CHANNEL_VEC intensity = max(front.intensity, back.intensity);
    float opacity = max(front.opacity, back.opacity);
    return IntegratedIntensity(intensity, opacity);
}

// Occluding projection
IntegratedIntensity integrate_occluding(in IntegratedIntensity front, in IntegratedIntensity back) 
{
    float opacity = 1.0 - (1.0 - front.opacity) * (1.0 - back.opacity);
    CHANNEL_VEC b = back.intensity * (1.0 - front.opacity/opacity);
    CHANNEL_VEC f = front.intensity * (front.opacity/opacity);
    return IntegratedIntensity(clamp(b + f, 0, 1), opacity);
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
    IntegratedIntensity intensity = IntegratedIntensity(CHANNEL_VEC(0), 0);
    bool rayIsFinished = false;
    float t0 = minRay;
    float coreParam = 0.0;
    float coreIntensity = 0.0;
    for (int s = 0; s < 1000; ++s) {
        float t1 = advance_to_voxel_edge(t0, 
                rayOriginInTexels, rayDirectionInTexels,
                rayBoxCorner, forwardMask, 
                texelsPerRay);
        if (t1 >= maxRay) {
            t1 = maxRay;
            rayIsFinished = true;
        }
        float t = mix(t0, t1, 0.5);
        vec3 texel = rayOriginInTexels + t * rayDirectionInTexels;
        vec3 texCoord = texel / texelsPerVolume;

        IntegratedIntensity rearIntensity = sample_nearest_neighbor(texCoord, levelOfDetail); // intentionally downsampled
        intensity = 
                integrate_max_intensity(intensity, rearIntensity);
                // integrate_occluding(intensity, rearIntensity);

        float tracingIntensity = dot(rearIntensity.intensity, tracingChannelMask);
        if (tracingIntensity >= coreIntensity) { // MIP criterion
            coreIntensity = tracingIntensity;
            coreParam = t;
        }

        if (rayIsFinished)
            break;
        t0 = t1;
    }

    // Secondary render target stores 16-bit core intensity, plus relative depth
    /* */
    float slabMin = intersectRayAndPlane(x0, x1, zNearPlaneInTexCoord);
    float slabMax = intersectRayAndPlane(x0, x1, zFarPlaneInTexCoord);
    float relativeDepth = (coreParam - slabMin) / (slabMax - slabMin);
    uint intDepth = uint(relativeDepth * 65535);
    /* */
    uint coreInt = uint(65535 * coreIntensity);
    coreDepth = ivec2(coreInt, intDepth); // OK

    // Primary render target stores final blended RGBA color
    fragColor =
            rgba_for_intensities(intensity);
}
