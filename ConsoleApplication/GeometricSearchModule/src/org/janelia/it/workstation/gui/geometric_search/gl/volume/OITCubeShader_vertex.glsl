#version 430

layout (location=0) in vec3 iv;

void main()
{
    gl_Position = vec4( iv.x, iv.y, iv.z, 1.0);
}