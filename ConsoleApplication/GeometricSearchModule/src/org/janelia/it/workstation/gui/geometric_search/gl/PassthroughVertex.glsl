#version 330

layout (location=0) in vec4 iv;

void main()
{
    gl_Position = iv;
}