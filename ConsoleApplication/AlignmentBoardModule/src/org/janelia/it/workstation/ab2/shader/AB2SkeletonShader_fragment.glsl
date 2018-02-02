#version 410

in vec3 oColor;
out vec4 color;

void main() {
    color = vec4(oColor.r, oColor.g, oColor.b, 1.0);
}
