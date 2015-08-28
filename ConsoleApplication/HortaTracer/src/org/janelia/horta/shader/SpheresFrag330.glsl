#version 330

/**
 * Sphere imposter fragment shader.
 */

uniform vec4 color = vec4(0, 1, 0, 1);
uniform mat4 projectionMatrix;


in vec3 imposterPos;
in float pc, c2; // linear coefficients
in vec3 center;
in float fragRadius;


out vec4 fragColor;


// defined in imposter_fns330.glsl
vec2 sphere_nonlinear_coeffs(vec3 pos, vec2 pc_c2);
vec3 sphere_surface_from_coeffs(vec3 pos, vec2 pc_c2, vec2 a2_d);
vec3 light_rig(vec4 pos, vec3 normal, vec3 color);
float fragDepthFromEyeXyz(vec3 eyeXyz, mat4 projectionMatrix);


void main() {
    vec2 pc_c2 = vec2(pc, c2);
    vec2 a2_d = sphere_nonlinear_coeffs(imposterPos, pc_c2);
    if (a2_d.y < 0)
        discard; // ray through point does not intersect sphere
    vec3 s = sphere_surface_from_coeffs(imposterPos, pc_c2, a2_d);
    vec3 normal = 1.0 / fragRadius * (s - center);
    fragColor = vec4(
        light_rig(vec4(s, 1), normal, color.rgb),
        1);
    gl_FragDepth = fragDepthFromEyeXyz(s, projectionMatrix);
}
