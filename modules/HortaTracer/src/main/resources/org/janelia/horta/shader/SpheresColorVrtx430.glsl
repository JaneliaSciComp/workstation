#version 430

/**
 * Vertex shader for truncated cone imposters.
 * Transforms World cone axis locations into View locations, prior to Geometry shader.
 */

layout(location = 1) uniform mat4 modelViewMatrix = mat4(1);
layout(location = 5) uniform float radiusOffset = 0.0;
layout(location = 6) uniform float radiusScale = 1.0;

layout(location = 1) in vec4 xyzr; // center of truncated cone end, and radius
layout(location = 2) in vec4 rgbv; // color and visibility

out vec4 geomRgbV; // pass color and visibility to geometry shader
out float geomRadius; // pass color and visibility to geometry shader

void main() {
    gl_Position = modelViewMatrix * vec4(xyzr.xyz, 1); // cone center position in camera frame
    geomRadius = radiusOffset + radiusScale * xyzr.w;
    geomRgbV = rgbv;
}
