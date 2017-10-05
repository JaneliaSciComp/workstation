#version 410

in vec4 vColor0;
in vec4 vColor1;
in vec3 tc_out;
flat in int textureTypeOut;
out vec4 color;

//  TEXTURE_TYPE_NONE=0;
//  TEXTURE_TYPE_2D_RGBA=1;
//  TEXTURE_TYPE_2D_R8=2;
//  TEXTURE_TYPE_3D_RGBA=3;

uniform sampler2D image_texture;
uniform sampler3D image_texture3d;

void main() {
  if (aRGBAt==1) {
    ivec2 imageSize=textureSize(image_texture, 0);
    ivec2 tcIv=ivec2(imageSize.x*tc_out.x, imageSize.y*tc_out.y);
    vec4 colorRgba=texelFetch(image_texture, tcIv, 0);
    color=colorRgba;
  } else if (aR8t==1) {
    ivec2 imageSize=textureSize(image_texture, 0);
    ivec2 tcIv=ivec2(imageSize.x*tc_out.x, imageSize.y*tc_out.y);
    vec4 colorRgba=texelFetch(image_texture, tcIv, 0);
    color=mix(vColor1, vColor0, colorRgba.r);
  } else {
    color = vColor0;
  }
}
