#version 410

in vec3 tc_out;
out vec4 color;

uniform sampler2DArray image_texture3d;

void main() {
  ivec3 imageSize3d=textureSize(image_texture3d, 0);
  ivec3 tci3=ivec3(imageSize3d.x*tc_out.x, imageSize3d.y*tc_out.y, imageSize3d.z*tc_out.z);
  color=texelFetch(image_texture3d, tci3, 0);
}
