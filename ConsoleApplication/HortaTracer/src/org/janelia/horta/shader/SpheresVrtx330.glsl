#version 330

/**
 * Vertex shader for sphere imposters.
 * Transforms World sphere locations into View locations, prior to Geometry shader.
 */


uniform mat4 modelViewMatrix = mat4(1);


in vec3 position; // center of sphere
in float radius; // radius of sphere


out float geomRadius; // pass radius to geometry shader


void main() {
    vec4 eyePos = modelViewMatrix * vec4(position, 1); // sphere center in camera frame
    gl_Position = eyePos;
    geomRadius = radius;
}
