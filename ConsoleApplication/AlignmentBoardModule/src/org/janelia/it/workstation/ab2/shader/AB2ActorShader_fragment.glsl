#version 410

in vec4 vColor0;
in vec4 vColor1;
in vec2 tc_out;
flat in int aRGBAt;
flat in int aR8t;
out vec4 color;

uniform sampler2D image_texture;

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
    color = vec4(0f, 0f, 1f, 1f);
  }
}
