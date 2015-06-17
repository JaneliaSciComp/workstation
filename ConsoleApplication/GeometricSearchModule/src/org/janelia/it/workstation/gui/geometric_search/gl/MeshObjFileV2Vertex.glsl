#version 330

uniform mat4 proj;
uniform mat4 view;
uniform mat4 model;

layout (location=0) in vec3 iv;
layout (location=1) in vec3 norm;

out vec3 N;
out vec3 I;
out vec4 Cs;

void main()
{
    Cs = vec4( iv.x+0.5f, iv.y+0.5f, iv.z, 1.0);
    vec4 centeredIv = vec4( iv.x, iv.y, iv.z, 1.0);
    mat4 mview = view * model;
    vec4 P = mview * centeredIv;
  	I  = P.xyz - vec3 (0);
  	mat3 normalMatrix = mat3(transpose(inverse(mview)));
  	N  = normalMatrix * norm;
  	gl_Position = proj * P;
}