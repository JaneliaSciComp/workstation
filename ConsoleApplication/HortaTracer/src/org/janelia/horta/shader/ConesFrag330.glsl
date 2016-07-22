#version 330

/**
 * Truncated cone imposter fragment shader.
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

uniform vec4 color = vec4(0, 1, 0, 1); // one color for all cones
uniform mat4 projectionMatrix; // needed for proper depth calculation
uniform sampler2D lightProbe; // for image-based-lighting (IBL)
uniform vec2 screenSize = vec2(1280, 800);

in float fragRadius; // average radius of cone
in vec3 center; // center of cone, in camera frame
in float taper; // change in radius per distance along cone axis
in vec3 halfAxis;
// the *linear* coefficients of the ray-tracing quadratic formula can be computed per-vertex, rather than per fragment.
in float tAP; // cone ray-casting quadratic-formula linear (actually constant) coefficient
in float qe_c; // cone ray-casting quadratic-formula linear coefficient
in float qe_half_b; // cone ray-casting quadratic-formula linear coefficient
in vec3 qe_undot_half_a; // cone ray-casting quadratic-formula linear coefficient
in vec3 imposterPos; // location of imposter bounding geometry, in camera frame
in float halfConeLength;
in vec3 aHat;
in float normalScale;
in float bViewAlongCone; // Is view angle less than taper angle?


out vec4 fragColor;


// forward declaraion of methods defined in imposter_fns330.glsl
void cone_nonlinear_coeffs(in float tAP, in float qe_c, in float qe_half_b, in vec3 qe_undot_half_a,
    out float qe_half_a, out float discriminant);
vec3 cone_surface_from_coeffs(in vec3 pos, in float qe_half_b, in float qe_half_a, in float discriminant, out vec3 back_surface);
vec3 light_rig(vec3 pos, vec3 normal, vec3 surface_color);
float fragDepthFromEyeXyz(vec3 eyeXyz, mat4 projectionMatrix);
float zNearFromProjection(mat4 projectionMatrix);
vec3 image_based_lighting(
        vec3 pos, // surface position, in camera frame
        vec3 normal, // surface normal, in camera frame
        vec3 diffuseColor, 
        vec3 reflectColor,
        sampler2D lightProbe);


void main() {
    vec3 s, normal; // surface and normal

    // set up quadratic formula to solve cone ray-casting equation
    float qe_half_a, discriminant;
    cone_nonlinear_coeffs(tAP, qe_c, qe_half_b, qe_undot_half_a, qe_half_a, discriminant);

    // float pixelSize = 2.0 / (screenSize.x * projectionMatrix[0][0]);
    if (discriminant < 0) {
        // At low zoom, don't discard: It causes artifacts
        // Compute pixel size, to determine current zoom level:
        vec4 shifted1 = projectionMatrix * (vec4(center, 1) + vec4(0.1, 0, 0, 0));
        vec4 shifted2 = projectionMatrix * (vec4(center, 1) - vec4(0.1, 0, 0, 0));
        float screensPerMicrometer = 5 * abs(shifted1.x/shifted1.w - shifted2.x/shifted2.w);
        float micrometersPerPixel = 1.0 / (screensPerMicrometer * screenSize.x);
        if (micrometersPerPixel > 0.2 * fragRadius) {
            // TODO: below assumes imposter is near the front surface of the quadric
            s = imposterPos;
            vec3 in_screen = normalize(cross(aHat, vec3(0,0,-1)));
            normal = normalize(cross(in_screen, aHat));
            if (normal.z < 0)
                normal = -normal;
        }
        else {
            discard;
        }
    }
    else {
        // Compute projected surface of cone
        // TODO - distinguish near-axis vs off-axis view cases
        vec3 back_surface;
        s = cone_surface_from_coeffs(imposterPos, qe_half_b, qe_half_a, discriminant,
             back_surface);
        vec3 cs = s - center;

        bool viewAlongCone = bViewAlongCone > 0.5;

        if (viewAlongCone) {
            // s = back_surface; // second surface is the seen one in this case
        }

        // Truncate cone geometry to prescribed ends
        if ( abs(dot(cs, aHat)) > halfConeLength ) {
            const bool doColorTipMissesMagenta = false;
            if (doColorTipMissesMagenta) { // for debugging
                fragColor = vec4(1, 0, 0.9, 1.0); // for debugging
                gl_FragDepth = 0.999;
                return;
            } else {
                gl_FragDepth = 1;
                discard; // past end of cone
            }
            discard;
        }

        // Compute surface normal vector, for shading
        vec3 n1 = normalize( cs - dot(cs, aHat)*aHat );
        normal = normalScale * (n1 + taper * aHat);
    }

    // Put computed cone surface Z depth into depth buffer
    gl_FragDepth = fragDepthFromEyeXyz(s, projectionMatrix);

    // Near clip to reveal solid core 
    /*
    if (gl_FragDepth < 0) {
        // Show nothing if rear surface is also closer than zNear
        if (! viewAlongCone) {
            float back_depth = fragDepthFromEyeXyz(back_surface, projectionMatrix);
            // if (back_depth < 0) discard;
        }
        gl_FragDepth = 0;
        s.z = zNearFromProjection(projectionMatrix); // Update clipped Z coordinate
        normal = vec3(0, 0, 1); // slice core parallel to screen
    } /* */

    // illuminate the cone surface
    vec3 reflectColor = mix(color.rgb, vec3(1,1,1), 0.5); // midway between metal and plastic.
    fragColor = vec4(
        image_based_lighting(s, normal, color.rgb, reflectColor, lightProbe),
        // light_rig(s, normal, color.rgb),
        color.a);
}
