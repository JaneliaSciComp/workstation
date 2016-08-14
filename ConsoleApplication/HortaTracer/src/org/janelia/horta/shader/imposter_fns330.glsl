#version 330

// Imposter shader, from project at https://github.com/cmbruns/swcimposters

/*
 * Copyright 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms ( http://license.janelia.org/license/jfrc_copyright_1_1.html ).
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

// TODO - parameterize these lighting parameters
const float specularCoefficient = 0.1; // range 0-1
const float diffuseCoefficient = 1.0 - specularCoefficient; // range 0-1
const float metallicCoefficient = 0.5; // range 0-1
const float roughnessCoefficient = 0.5; // range 0-1, currently unused TODO

// color patch using image based lighting, using only diffuse and reflection components, for now.
vec3 image_based_lighting(
        vec3 pos, // surface position, in camera frame
        vec3 normal, // surface normal, in camera frame
        vec3 diffuseColor, 
        vec3 reflectColor,
        sampler2D lightProbe)
{
    // Assume light probe has diffuse probe on left, reflection probe on right
    const vec4 diffuseTcPos = vec4(0.25, 0.5, 0.245, -0.49); // center and radius of light probe image
    const vec4 reflectTcPos = vec4(0.75, 0.5, 0.245, -0.49); // center and radius of light probe image

    // convert normal vector to position in diffuse light probe texture
    float radius = 0.50 * (-normal.z + 1.0);
    vec2 direction = normal.xy;
    if (dot(direction, direction) > 0)
        direction = normalize(direction);
    vec2 diffuseTc = diffuseTcPos.xy + diffuseTcPos.zw * radius * direction;
    vec3 iblDiffuse = diffuseCoefficient * texture(lightProbe, diffuseTc).rgb;

    // convert position and normal to position in reflection light probe texture
    vec3 view = pos;
    vec3 r = normalize(view - 2.0 * dot(normal, view) * normal); // reflection vector
    radius = 0.50 * (-r.z + 1.0);
    direction = normal.xy;
    if (dot(direction, direction) > 0)
        direction = normalize(direction);
    vec2 reflectTc = reflectTcPos.xy + reflectTcPos.zw * radius * direction;
    vec3 iblReflect = 35 * specularCoefficient * texture(lightProbe, reflectTc).rgb;

    return iblDiffuse * diffuseColor + iblReflect * reflectColor;
}


// Hard coded light system, just for testing.
// Light parameters should be same ones in CPU host program, for comparison
vec3 light_rig(vec3 pos, vec3 normal, vec3 surface_color) {
    const vec3 ambient_light = vec3(0.2, 0.2, 0.2);
    const vec3 diffuse_light = vec3(0.8, 0.8, 0.8);
    const vec3 specular_light = vec3(0.8, 0.8, 0.8);
    const vec4 light_pos = vec4(-5, 3, 3, 0); 
    // const vec3 surface_color = vec3(1, 0.5, 1);

    vec3 surfaceToLight = normalize(light_pos.xyz);
    
    float diffuseCoefficient = max(0.0, dot(normal, surfaceToLight));
    vec3 diffuse = diffuseCoefficient * surface_color * diffuse_light;

    vec3 ambient = ambient_light * surface_color;
    
    vec3 surfaceToCamera = normalize(-pos); //also a unit vector    
    // Use Blinn-Phong specular model, to match fixed-function pipeline result (at least on nvidia)
    vec3 H = normalize(surfaceToLight + surfaceToCamera);
    float nDotH = max(0.0, dot(normal, H));
    float specularCoefficient = pow(nDotH, 100);
    vec3 specular = specularCoefficient * specular_light;

    return diffuse + specular + ambient;        
}

float fragDepthFromEyeXyz(vec3 eyeXyz, mat4 projectionMatrix) {
    // From http://stackoverflow.com/questions/10264949/glsl-gl-fragcoord-z-calculation-and-setting-gl-fragdepth
    // NOTE: change far and near to constant 1.0 and 0.0 might be worth trying for performance optimization
    float far=gl_DepthRange.far; // usually 1.0
    float near=gl_DepthRange.near; // usually 0.0

    vec4 eye_space_pos = vec4(eyeXyz, 1);
    vec4 clip_space_pos = projectionMatrix * eye_space_pos;
    
    float ndc_depth = clip_space_pos.z / clip_space_pos.w;
    
    float depth = (((far-near) * ndc_depth) + near + far) / 2.0;
    return depth;
}


float zNearFromProjection(mat4 projectionMatrix) {
    float m22 = projectionMatrix[2][2];
    float m32 = projectionMatrix[3][2];
    float near = (2.0f*m32)/(2.0*m22-2.0);
    // float far = ((m22-1.0)*near)/(m22+1.0);
    return -near;
}


// CONES
// Methods for ray casting cone geometry from imposter geometry

// First phase of cone imposter shading: Compute linear coefficients in vertex shader,
// to ease burden on fragment shader
void cone_linear_coeffs(in vec3 center, in float radius, in vec3 axis, in float taper, in vec3 pos,
        out float tAP, out float qe_c, out float qe_half_b, out vec3 qe_undot_half_a) 
{
    // Scale computations for better numerical stability?
    float scale1 = 20.0 / length(center); // center is constant over entire cone
    float scale2 = scale1 * scale1;

    // x = Unit cylinder axis
    vec3 x = normalize(-axis); // minus is important...
    
    // "A" parameter of quadratic formula is nonlinear, but has two linear components
    // Q: Can we use scalar instead of vector? A: No
    qe_undot_half_a = cross(scale1 * pos, x); // (1)
    
    // "B" parameter
    tAP = taper * dot(x, scale1 * pos); // (2)
    float tAC = taper * dot(x, scale1 * center);
    vec3 qe_undot_b_part = vec3(
        dot(center, vec3(-x.y*x.y -x.z*x.z, x.x*x.y, x.x*x.z)),
        dot(center, vec3( x.x*x.y, -x.x*x.x - x.z*x.z, x.y*x.z)),
        dot(center, vec3( x.x*x.z,  x.y*x.z, -x.x*x.x - x.y*x.y)));
    float rad_s = scale1 * radius;
    qe_half_b = dot(scale2 * pos, qe_undot_b_part) - tAP * (rad_s - tAC);
    
    // "C" parameter of quadratic formula is complicated, but is constant wrt position
    vec3 cxa = cross(scale1 * center, x);
    qe_c = dot(cxa, cxa) - rad_s * rad_s + (2*rad_s - tAC)*tAC;
}

// Second phase of sphere imposter shading: Compute nonlinear coefficients
// in fragment shader, including discriminant used to reject fragments.
void cone_nonlinear_coeffs(in float tAP, in float qe_c, in float qe_half_b, in vec3 qe_undot_half_a,
    out float qe_half_a, out float discriminant) 
{
    // set up quadratic formula for sphere surface ray casting
    qe_half_a = dot(qe_undot_half_a, qe_undot_half_a) - tAP * tAP;
    discriminant = qe_half_b * qe_half_b - qe_half_a * qe_c;
}

// Third and final phase of sphere imposter shading: Compute sphere
// surface XYZ coordinates in fragment shader.
vec3 cone_surface_from_coeffs(in vec3 pos, in float qe_half_b, in float qe_half_a, in float discriminant,
        out vec3 back_surface)
{
    float left = -qe_half_b / qe_half_a;
    float right = sqrt(discriminant) / qe_half_a;
    float alpha1 = left - right; // near surface of cone
    float alpha2 = left + right; // far/back surface of cone
    back_surface = alpha2 * pos;
    // TODO - case when looking down cone axis
    vec3 surface_pos = alpha1 * pos;
    return surface_pos;
}

// Convenience fragment shader method for cone imposters
// Returns false if fragment should be discarded
bool cone_imposter_frag(
        in vec3 surface_color,
        in vec3 pos, // location of imposter geometry fragment
        in vec3 aHat, // unit cone axis
        in float halfConeLength,
        in vec3 center,
        in float taper,
        in float tAP,
        in float qe_c,
        in float qe_half_b,
        in vec3 qe_undot_half_a, 
        in float normalScale,
        in mat4 projectionMatrix,
        out vec4 fragColor,
        out float fragDepth)
{
    // Cull unneeded fragments by setting up quadratic formula
    float qe_half_a, discriminant;
    cone_nonlinear_coeffs(tAP, qe_c, qe_half_b, qe_undot_half_a,
        qe_half_a, discriminant);
    if (discriminant <= 0)
        return false; // Point does not intersect cone

    // Compute projected surface of cone
    vec3 back_surface;
    vec3 s = cone_surface_from_coeffs(pos, qe_half_b, qe_half_a, discriminant, back_surface);
    vec3 cs = s - center;
    
    // Truncate cone geometry to prescribed ends
    if ( abs(dot(cs, aHat)) > halfConeLength ) 
        return false;
    
    // Compute surface normal vector, for shading
    vec3 n1 = normalize( cs - dot(cs, aHat)*aHat );
    vec3 normal = normalScale * (n1 + taper * aHat);

    // illuminate the cone surface
    fragColor = vec4(
        light_rig(s, normal, surface_color),
        1);

    // Put computed cone surface Z depth into depth buffer
    fragDepth = fragDepthFromEyeXyz(s, projectionMatrix);
    return true;
}

// SPHERES
// Methods for ray casting sphere geometry from imposter geometry


// First phase of sphere imposter shading: Compute linear coefficients in vertex shader,
// to ease burden on fragment shader
vec2 sphere_linear_coeffs(vec3 center, float radius, vec3 pos) {
    float pc = dot(pos, center);
    float c2 = dot(center, center) - radius * radius;
    return vec2(pc, c2);
}

// Second phase of sphere imposter shading: Compute nonlinear coefficients
// in fragment shader, including discriminant used to reject fragments.
vec2 sphere_nonlinear_coeffs(vec3 pos, float pc, float c2) {
    // set up quadratic formula for sphere surface ray casting
    float a2 = dot(pos, pos);
    float discriminant = pc*pc - a2*c2; // quadratic formula discriminant: b^2 - 4ac
    return vec2(a2, discriminant);
}

// Third and final phase of sphere imposter shading: Compute sphere
// surface XYZ coordinates in fragment shader.
vec3 sphere_surface_from_coeffs(vec3 pos, float pc, vec2 a2_d, out vec3 back_surface) {
    float discriminant = a2_d.y; // Negative values should be discarded.
    float a2 = a2_d.x;
    float b = pc;
    float left = b / a2; // left half of quadratic formula: -b/2a
    float right = sqrt(discriminant) / a2; // (negative) right half of quadratic formula: sqrt(b^2-4ac)/2a
    float alpha1 = left - right; // near/front surface of sphere
    float alpha2 = left + right; // far/back surface of sphere
    back_surface = alpha2 * pos;
    return alpha1 * pos;
}

