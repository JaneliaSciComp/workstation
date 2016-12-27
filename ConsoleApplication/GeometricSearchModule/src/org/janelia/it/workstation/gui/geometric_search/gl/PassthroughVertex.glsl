#version 330

layout (location=0) in vec4 iv;

uniform mat4 proj;
uniform mat4 view;
uniform mat4 model;

out vec4 position2;

void main()
{
    position2 = vec4( iv.x+0.5f, iv.y+0.5f, iv.z, 1.0);
    vec4 centeredIv = vec4( iv.x, iv.y, iv.z, 1.0);
    gl_Position = proj * view * model * centeredIv;
}