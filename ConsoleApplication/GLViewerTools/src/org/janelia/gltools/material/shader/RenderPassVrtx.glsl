#version 330

/**
 * OpenGL shading language shader
 * Generic Vertex shader for render passes that read from a texture image,
 * and use a ScreenQuadMesh for geometry
 */

in vec3 position;
in vec2 texCoord;
// ignore color field for now...

out vec2 screenCoord;

void main() {
    gl_Position = vec4(position, 1);
    screenCoord = texCoord;
}
