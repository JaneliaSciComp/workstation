#version 430

layout (location=0) in vec3 iv;

out float vz;

void main()
{
    vz = iv.z;
    gl_Position = vec4( iv.x, iv.y, iv.z, 1.0);
}