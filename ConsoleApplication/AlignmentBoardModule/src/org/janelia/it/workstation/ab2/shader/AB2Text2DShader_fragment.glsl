#version 410

in vec3 tc_out;

out vec4 color;

uniform sampler2D image_texture;
uniform vec4 colorForeground;
uniform vec4 colorBackground;

void main() {

  ivec2 imageSize2d;
  ivec2 tci2;

  imageSize2d=textureSize(image_texture, 0);
  tci2=ivec2(imageSize2d.x*tc_out.x, imageSize2d.y*tc_out.y);
  vec4 colorRgba=texelFetch(image_texture, tci2, 0);
  //color=mix(colorForeground, colorBackground, colorRgba.r);
  color=mix(colorBackground, colorForeground, colorRgba.r);
}