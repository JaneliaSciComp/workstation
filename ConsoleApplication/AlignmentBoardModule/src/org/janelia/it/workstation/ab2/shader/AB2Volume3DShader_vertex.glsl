#version 410

layout (location=0) in vec3 iv;
layout (location=1) in vec3 tc;

uniform mat4 textureMvp3d;
uniform mat4 mvp3d;

out vec3 tc_out;

void main()
{
  vec4 vp = vec4(iv, 1.0);
  gl_Position = mvp3d * vp;
  vec4 tc4=vec4(tc, 1.0);
  vec4 tc_out4 = textureMvp3d * tc4;
  tc_out = tc_out4.xyz;
}
