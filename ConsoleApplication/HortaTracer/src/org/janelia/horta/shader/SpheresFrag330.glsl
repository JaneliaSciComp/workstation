#version 330

/**
 * Sphere imposter fragment shader.
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

uniform vec4 color = vec4(0, 1, 0, 1); // one color for all spheres
uniform mat4 projectionMatrix; // needed for proper sphere depth calculation
uniform sampler2D lightProbe;


in vec3 imposterPos; // imposter geometry location, in camera frame
in float pc, c2; // pre-computed ray-casting quadratic formula linear coefficients
in vec3 center; // sphere center in camera frame
in float fragRadius; // sphere radius


out vec4 fragColor;


// methods defined in imposter_fns330.glsl
vec2 sphere_nonlinear_coeffs(vec3 pos, float pc, float c2); // sphere surface ray-casting intermediate parameters
vec3 sphere_surface_from_coeffs(vec3 pos, float pc, vec2 a2_d, out vec3 back_surface); //
vec3 light_rig(vec3 pos, vec3 normal, vec3 color); // simple hard-coded shading, for testing
float fragDepthFromEyeXyz(vec3 eyeXyz, mat4 projectionMatrix); // computes correct sphere depth-buffer value
vec3 image_based_lighting(
        vec3 pos, // surface position, in camera frame
        vec3 normal, // surface normal, in camera frame
        vec3 diffuseColor, 
        vec3 reflectColor,
        sampler2D lightProbe);


void main() {
    vec2 a2_d = sphere_nonlinear_coeffs(imposterPos, pc, c2);
    // fast-fail rays that miss sphere, before expensively solving exact surface location
    if (a2_d.y < 0) // quadratic formula discriminant, (b^2 - 4ac) < 0
        discard; // ray through point does not intersect sphere
    vec3 back_surface;
    vec3 s = sphere_surface_from_coeffs(imposterPos, pc, a2_d, back_surface);
    vec3 normal = 1.0 / fragRadius * (s - center); // normalized without an additional sqrt! :)
    gl_FragDepth = fragDepthFromEyeXyz(s, projectionMatrix);

    // Near clip to reveal solid core
    if (gl_FragDepth < 0) { // Near surface is clipped by zNear
        // Show nothing if rear surface is also closer than zNear
        float back_depth = fragDepthFromEyeXyz(back_surface, projectionMatrix);
        if (back_depth <= 0) {
            discard;
        }
        gl_FragDepth = 0;
        // s.z = ?; // TODO - what's zNear in scene units?
        normal = vec3(0, 0, 1); // slice core parallel to screen
    }

    // Color and shading
    vec3 reflectColor = mix(color.rgb, vec3(1,1,1), 0.5); // midway between metal and plastic.
    fragColor = vec4(
        image_based_lighting(s, normal, color.rgb, reflectColor, lightProbe),
        // light_rig(s, normal, color.rgb),
        color.a);
}
