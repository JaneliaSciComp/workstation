#version 330

/**
 * Vertex shader for sphere imposters.
 * Transforms World sphere locations into View locations, prior to Geometry shader.
 */



uniform mat4 modelViewMatrix = mat4(1);
uniform float radiusOffset = 0.0;
uniform float radiusScale = 1.0;


in vec3 position; // center of sphere
in float radius; // radius of sphere


out float geomRadius; // pass radius to geometry shader


void main() {
    gl_Position = modelViewMatrix * vec4(position, 1); // sphere center in camera frame
    geomRadius = radiusOffset + radiusScale * radius;
}
