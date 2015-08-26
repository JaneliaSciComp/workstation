#version 430

layout (location=0) in vec3 iv;
layout (location=1) in float intensityV;

out float intensityG;

void main()
{
    intensityG=intensityV;
    //gl_Position = vec4( iv.x, iv.y, iv.z, 1.0);
    gl_Position = vec4(0.0, 0.0, 0.0, 1.0);
}