#version 430

uniform mat4 proj;
uniform mat4 mv;
uniform mat4 nm;

uniform vec4 dcolor;

layout (location=0) in vec3 iv;
layout (location=1) in vec3 norm;

out vec3 N;
out vec3 I;
out vec4 Cs;

out float vz;

void main()
{
    Cs = vec4(dcolor.x, dcolor.y, dcolor.z, 1.0);
    vec4 centeredIv = vec4( iv.x, iv.y, iv.z, 1.0);
    vec4 P = mv * centeredIv;
    I  = P.xyz - vec3 (0);
    mat3 normalMatrix = mat3(nm);
    N  = normalMatrix * norm;
    gl_Position = proj * P;
    vz = gl_Position.z;
}