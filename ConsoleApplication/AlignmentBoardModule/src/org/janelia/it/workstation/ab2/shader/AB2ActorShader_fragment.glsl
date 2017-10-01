#version 410

in vec4 vColor;
in vec2 tc_out;
flat in int ait;
out vec4 color;

uniform sampler2D image_texture;

void main() {
  if (ait==0) {
    color = vColor;
  } else {
    ivec2 imageSize=textureSize(image_texture, 0);
    ivec2 tcIv=ivec2(imageSize.x*tc_out.x, imageSize.y*tc_out.y);
    vec4 colorRgba=texelFetch(image_texture, tcIv, 0);
    color=colorRgba;
  }
}
