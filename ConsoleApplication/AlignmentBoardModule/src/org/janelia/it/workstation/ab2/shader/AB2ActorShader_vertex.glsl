#version 410

layout (location=0) in vec3 iv;
layout (location=1) in vec3 tc;

uniform mat4 mvp3d;
uniform mat4 mvp2d;
uniform vec4 color0;
uniform vec4 color1;
uniform int twoDimensional;
uniform int textureType;

out vec4 vColor0;
out vec4 vColor1;
out vec3 tc_out;
flat out int textureTypeOut;

void main()
{
  vec4 vp = vec4(iv.x, iv.y, iv.z, 1.0);
  switch(twoDimensional) {
  case 1:
     vp.z=0.0;
     gl_Position = mvp2d * vp;
     break;
  default:
     gl_Position = mvp3d * vp;
  }
  vColor0=color0;
  vColor1=color1;
  textureTypeOut=textureType;
  tc_out=tc;
}

