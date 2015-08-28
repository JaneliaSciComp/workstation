#version 330

/**
 * Vertex shader for sphere imposters.
 * Transforms World sphere locations into View locations, prior to Geometry shader.
 */


uniform mat4 modelViewMatrix = mat4(1);
uniform float particleScale = 10.0; // pixels per scene unit?
// uniform vec4 color;


in vec3 position;
in float radius;

out float geomRadius;


void main() {
    vec4 eyePos = modelViewMatrix * vec4(position, 1);
    gl_Position = eyePos;
    geomRadius = radius;
}
