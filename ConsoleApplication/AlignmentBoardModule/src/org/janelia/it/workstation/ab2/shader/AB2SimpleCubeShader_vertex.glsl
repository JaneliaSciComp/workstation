#version 410

uniform mat4 mvp;

void main()
{
  vec4 p = vec4(0.0, 0.0, 0.5, 1.0);
  gl_Position = mvp * p;
  //gl_Position = p;
}
