#version 410

layout (location=0) in vec3 iv;
layout (location=1) in vec2 tc;

uniform mat4 mvp;
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
     gl_Position=vec4(iv.x, iv.y, 0.0, 1.0);
  } else {
     gl_Position = mvp * vp;
  }
  vColor0=color0;
  vColor1=color1;
  aRGBAt=applyImageRGBATexture;
  aR8t=applyImageR8Texture;
  tc_out=tc;
}

