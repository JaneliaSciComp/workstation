#version 330

/**
 * Sphere imposter fragment shader.
 */

uniform vec4 color = vec4(0, 1, 0, 1);


in vec3 imposterPos;
in float pc, c2; // linear coefficients


// defined in imposter_fns120.glsl
vec2 sphere_nonlinear_coeffs(vec3 pos, vec2 pc_c2);


void main() {
    vec2 a2_d = sphere_nonlinear_coeffs(imposterPos, vec2(pc, c2));
    if (a2_d.y < 0)
        discard; // ray through point does not intersect sphere

    gl_FragColor = color;
}
