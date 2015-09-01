#version 330

/**
 * Sphere imposter fragment shader.
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
vec3 sphere_surface_from_coeffs(vec3 pos, float pc, vec2 a2_d); //
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
    vec3 s = sphere_surface_from_coeffs(imposterPos, pc, a2_d);
    vec3 normal = 1.0 / fragRadius * (s - center); // normalized without an additional sqrt! :)
    fragColor = vec4(
        image_based_lighting(s, normal, color.rgb, color.rgb, lightProbe),
        // light_rig(s, normal, color.rgb),
        color.a);
    gl_FragDepth = fragDepthFromEyeXyz(s, projectionMatrix);
}
