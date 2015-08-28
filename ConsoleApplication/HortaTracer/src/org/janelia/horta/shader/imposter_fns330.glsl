#version 330

// Imposter shader, from project at https://github.com/cmbruns/swcimposters

/*
 * Copyright 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms ( http://license.janelia.org/license/jfrc_copyright_1_1.html ).
 */

// Hard coded light system, just for testing.
// Light parameters should be same ones in CPU host program, for comparison
vec3 light_rig(vec4 pos, vec3 normal, vec3 surface_color) {
    const vec3 ambient_light = vec3(0.2, 0.2, 0.2);
    const vec3 diffuse_light = vec3(0.8, 0.8, 0.8);
    const vec3 specular_light = vec3(0.8, 0.8, 0.8);
    const vec4 light_pos = vec4(-5, 3, 3, 0); 
    // const vec3 surface_color = vec3(1, 0.5, 1);

    vec3 surfaceToLight = normalize(light_pos.xyz); //  - (pos / pos.w).xyz);
    
    float diffuseCoefficient = max(0.0, dot(normal, surfaceToLight));
    vec3 diffuse = diffuseCoefficient * surface_color * diffuse_light;

    vec3 ambient = ambient_light * surface_color;
    
    vec3 surfaceToCamera = normalize(-pos.xyz); //also a unit vector    
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


// CONES
// Methods for ray casting cone geometry from imposter geometry

// First phase of cone imposter shading: Compute linear coefficients in vertex shader,
// to ease burden on fragment shader
void cone_linear_coeffs(in vec3 center, in float radius, in vec3 axis, in float taper, in vec3 pos,
        out float tAP, out float qe_c, out float qe_half_b, out vec3 qe_undot_half_a) 
{
    // x = Unit cylinder axis
    vec3 x = normalize(-axis); // minus is important...
    
    // "A" parameter of quadratic formula is nonlinear, but has two linear components
    // Q: Can we use scalar instead of vector? A: No
    qe_undot_half_a = cross(pos, x); // (1)
    
    // "B" parameter
    tAP = taper * dot(x, pos); // (2)
    float tAC = taper * dot(x, center);
    vec3 qe_undot_b_part = vec3(
        dot(center, vec3(-x.y*x.y -x.z*x.z, x.x*x.y, x.x*x.z)),
        dot(center, vec3( x.x*x.y, -x.x*x.x - x.z*x.z, x.y*x.z)),
        dot(center, vec3( x.x*x.z,  x.y*x.z, -x.x*x.x - x.y*x.y)));
    qe_half_b = dot(pos, qe_undot_b_part) - tAP * (radius - tAC);
    
    // "C" parameter of quadratic formula is complicated, but is constant wrt position
    vec3 cxa = cross(center, x);
    qe_c = dot(cxa, cxa) - radius * radius + (2*radius - tAC)*tAC;
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
vec3 cone_surface_from_coeffs(in vec3 pos, in float qe_half_b, in float qe_half_a, in float discriminant)
{
    float left = -qe_half_b / qe_half_a;
    float right = sqrt(discriminant) / qe_half_a;
    float alpha1 = left - right; // near surface of sphere
    // float alpha2 = left + right; // far/back surface of sphere
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
    vec3 s = cone_surface_from_coeffs(pos, qe_half_b, qe_half_a, discriminant);
    vec3 cs = s - center;
    
    // Truncate cone geometry to prescribed ends
    if ( abs(dot(cs, aHat)) > halfConeLength ) 
        return false;
    
    // Compute surface normal vector, for shading
    vec3 n1 = normalize( cs - dot(cs, aHat)*aHat );
    vec3 normal = normalScale * (n1 + taper * aHat);

    // illuminate the cone surface
    fragColor = vec4(
        light_rig(vec4(s, 1), normal, surface_color),
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
vec2 sphere_nonlinear_coeffs(vec3 pos, vec2 pc_c2) {
    // set up quadratic formula for sphere surface ray casting
    float b = pc_c2.x;
    float a2 = dot(pos, pos);
    float c2 = pc_c2.y;
    float discriminant = b*b - a2*c2;
    return vec2(a2, discriminant);
}

// Third and final phase of sphere imposter shading: Compute sphere
// surface XYZ coordinates in fragment shader.
vec3 sphere_surface_from_coeffs(vec3 pos, vec2 pc_c2, vec2 a2_d) {
    float discriminant = a2_d.y; // Negative values should be discarded.
    float a2 = a2_d.x;
    float b = pc_c2.x;
    float left = b / a2;
    float right = sqrt(discriminant) / a2;
    float alpha1 = left - right; // near surface of sphere
    // float alpha2 = left + right; // far/back surface of sphere
    return alpha1 * pos;
}

