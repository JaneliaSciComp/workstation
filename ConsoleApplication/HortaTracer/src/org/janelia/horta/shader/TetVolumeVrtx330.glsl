#version 430 core

/*
 * Vertex shader for tetrahedral volume rendering
 */

layout(location = 0) uniform mat4 modelViewMatrix = mat4(1);

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 texCoord;

out vec3 geomTexCoord;

void main() {
    // convert world coordinate to camera coordinate
    gl_Position = modelViewMatrix * vec4(position, 1);
    geomTexCoord = texCoord;
}
