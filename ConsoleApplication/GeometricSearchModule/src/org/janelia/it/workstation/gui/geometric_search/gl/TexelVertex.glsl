#version 330

layout (location=0) in vec4 in_position;
layout (location=1) in vec2 in_tex_coord;

out vec2 vs_tex_coord;

void main() {
    gl_Position = in_position;
    vs_tex_coord = in_tex_coord;
}