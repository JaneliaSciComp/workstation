#version 410

layout (location=0) in vec4 iv;
layout (location=1) in vec3 iColor;

uniform mat4 mvp;

out vec3 oColor;

void main()
{
  gl_Position = mvp * iv;
  oColor=iColor;
}

