#version 410

layout (location=0) in vec3 iv;

uniform mat4 mvp;
uniform vec4 styleIdColor;

out vec4 vColor;

void main()
{
  vec4 vp = vec4(iv.x, iv.y, iv.z, 1.0);
  gl_Position = mvp * vp;
  vColor=styleIdColor;
}

