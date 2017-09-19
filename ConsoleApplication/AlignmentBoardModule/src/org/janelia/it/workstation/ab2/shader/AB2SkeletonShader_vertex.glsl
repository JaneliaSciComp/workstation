#version 430

layout (location=0) in vec3 iv;
layout (location=1) in vec3 iColor;

uniform mat4 mvp;

out vec3 oColor;

void main()
{
  vec4 vp = vec4(iv.x, iv.y, iv.z, 1.0);
  gl_Position = mvp * vp;
  oColor=iColor;
}

