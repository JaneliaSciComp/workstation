#version 410

layout (location=0) in vec3 iv;

uniform mat4 mvp2d;
uniform vec4 color0;

out vec4 color0v;

void main()
{
  vec4 vp = vec4(iv, 1.0);
  vp.z=0.0;
  gl_Position = mvp2d * vp;
  color0v=color0;
}
