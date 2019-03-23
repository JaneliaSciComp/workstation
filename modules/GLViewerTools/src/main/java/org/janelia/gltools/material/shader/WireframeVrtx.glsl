#version 330

// Advanced wireframe vertex shader

uniform mat4 modelViewMatrix = mat4(1);

in vec3 position;

void main() {
    gl_Position = modelViewMatrix * vec4(position, 1);
}
