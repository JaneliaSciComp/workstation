#version 410

layout (location=0) in vec3 iv;
layout (location=1) in vec3 tc;

uniform mat4 mvp2d;

out vec3 tc_out;

void main()
{
  vec4 vp = vec4(iv, 1.0);
  vp.z=0.0;
  gl_Position = mvp2d * vp;
  tc_out=tc;
}