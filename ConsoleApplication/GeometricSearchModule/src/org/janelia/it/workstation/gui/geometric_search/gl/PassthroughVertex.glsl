#version 330

layout (location=0) in vec4 iv;

uniform mat4 proj;
uniform mat4 view;
uniform mat4 model;

void main()
{
    gl_Position = model * view * proj * iv;
}