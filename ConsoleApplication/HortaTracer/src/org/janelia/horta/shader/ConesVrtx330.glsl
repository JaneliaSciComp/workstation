#version 330

/**
 * Vertex shader for truncated cone imposters.
 * Transforms World cone axis locations into View locations, prior to Geometry shader.
 */


uniform mat4 modelViewMatrix = mat4(1);


in vec3 position; // center of truncated cone end
in float radius; // radius of truncated cone end


out float geomRadius; // pass radius to geometry shader


void main() {
    gl_Position = modelViewMatrix * vec4(position, 1); // cone center position in camera frame
    geomRadius = radius;
}
