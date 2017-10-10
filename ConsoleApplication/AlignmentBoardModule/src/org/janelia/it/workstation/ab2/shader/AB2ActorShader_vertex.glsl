#version 410

layout (location=0) in vec3 iv;
layout (location=1) in vec3 tc;

uniform mat4 textureMvp3d;
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
  vec4 vp = vec4(iv, 1.0);
  switch(twoDimensional) {
  case 1:
     vp.z=0.0;
     gl_Position = mvp2d * vp;
     tc_out=tc;
     break;
  default:
     gl_Position = mvp3d * vp;
     vec4 tc4=vec4(tc, 1.0);
     vec4 tc_out4 = textureMvp3d * tc4;
     tc_out = tc_out4.xyz;
  }
  vColor0=color0;
  vColor1=color1;
  textureTypeOut=textureType;
}

