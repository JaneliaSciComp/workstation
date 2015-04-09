#version 330

uniform vec4 outlineColor = vec4(0, 0, 0, 1);

out vec4 fragColor;

// Advanced wireframe fragment shader
void main() {
    fragColor = outlineColor;
}
