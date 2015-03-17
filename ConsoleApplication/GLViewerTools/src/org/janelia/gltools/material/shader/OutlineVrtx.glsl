#version 330

// Outline/silhouette vertex shader

uniform mat4 modelViewMatrix = mat4(1);

in vec3 position;

void main() {
    // Geometry shader wants orthogonal camera space coordinates,
    // for front-face calculation, and perspective-correct outline-thickness.
    gl_Position = modelViewMatrix * vec4(position, 1);
}
