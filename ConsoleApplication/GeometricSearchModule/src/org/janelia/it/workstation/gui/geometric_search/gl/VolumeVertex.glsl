#version 430

uniform mat4 proj;
uniform mat4 view;
uniform mat4 model;

void main()
{
  vec4 p = vec4(0.5, 0.5, 0.5, 1.0);
  gl_Position = proj * view * model * p;
}