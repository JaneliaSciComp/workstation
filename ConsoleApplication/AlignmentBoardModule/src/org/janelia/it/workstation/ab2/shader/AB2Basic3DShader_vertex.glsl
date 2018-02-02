#version 410

uniform mat4 mvp;
uniform vec4 color0;

layout (location=0) in vec3 iv;

out vec4 color0v;

void main()
{
  vec4 p = vec4(iv, 1.0);
  gl_Position = mvp * p;
  color0v=color0;
}