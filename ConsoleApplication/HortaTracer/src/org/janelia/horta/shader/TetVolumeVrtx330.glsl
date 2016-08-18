#version 330 core

/*
 * Vertex shader for tetrahedral volume rendering
 */

uniform mat4 modelViewMatrix = mat4(1);

in vec3 position;

void main() {
    // convert world coordinate to camera coordinate
    gl_Position = modelViewMatrix * vec4(position, 1); 
}
