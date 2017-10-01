#version 410

layout (location=0) in vec3 iv;
layout (location=1) in vec2 tc;

uniform mat4 mvp;
uniform vec4 styleIdColor;
uniform int twoDimensional;
uniform int applyImageTexture;

out vec4 vColor;
out vec2 tc_out;
flat out int ait;

void main()
{
  vec4 vp = vec4(iv.x, iv.y, iv.z, 1.0);
  if (twoDimensional==1) {
     gl_Position=vec4(iv.x, iv.y, 0.0, 1.0);
  } else {
     gl_Position = mvp * vp;
  }
  vColor=styleIdColor;
  ait=applyImageTexture;
  tc_out=tc;
}

