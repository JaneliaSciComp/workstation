#version 330

/**
 * Sphere imposter fragment shader.
 */

uniform vec4 color = vec4(0, 1, 0, 1);

void main() {
    gl_FragColor = color;
}
