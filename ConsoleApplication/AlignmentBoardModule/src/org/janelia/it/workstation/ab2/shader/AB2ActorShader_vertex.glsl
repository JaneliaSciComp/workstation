#version 410

layout (location=0) in vec3 iv;
layout (location=1) in vec2 tc;

uniform mat4 mvp3d;
uniform mat4 mvp2d;
uniform vec4 color0;
uniform vec4 color1;
uniform int twoDimensional;
uniform int applyImageRGBATexture;
uniform int applyImageR8Texture;

out vec4 vColor0;
out vec4 vColor1;
out vec2 tc_out;
flat out int aRGBAt;
flat out int aR8t;

void main()
{
  vec4 vp = vec4(iv.x, iv.y, iv.z, 1.0);
  if (twoDimensional==1) {
     vp.z=0.0;
     gl_Position = mvp2d * vp;
  } else {
     gl_Position = mvp3d * vp;
  }
  vColor0=color0;
  vColor1=color1;
  aRGBAt=applyImageRGBATexture;
  aR8t=applyImageR8Texture;
  tc_out=tc;
}

