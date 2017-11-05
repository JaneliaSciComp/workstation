#version 410

in vec3 tc_out;

out vec4 color;

uniform sampler2D image_texture;

void main() {

  ivec2 imageSize2d;
  ivec2 tci2;

  imageSize2d=textureSize(image_texture, 0);
  tci2=ivec2(imageSize2d.x*tc_out.x, imageSize2d.y*tc_out.y);
  color=texelFetch(image_texture, tci2, 0);
}

