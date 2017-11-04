#version 410

in vec3 tc_out;
out vec4 color;

uniform vec3 image_dim;
uniform float image_max_dim;

uniform sampler2DArray image_texture3d;

void main() {
  //ivec3 imageSize3d=textureSize(image_texture3d, 0);
  float midMax=image_max_dim/2.0;
  float xNormPos=(image_dim.x/2.0) + (tc_out.x*image_max_dim-midMax);
  float yNormPos=(image_dim.y/2.0) + (tc_out.y*image_max_dim-midMax);
  float zNormPos=(image_dim.z/2.0) + (tc_out.z*image_max_dim-midMax);

  //ivec3 tci3=ivec3(imageSize3d.x*tc_out.x, imageSize3d.y*tc_out.y, imageSize3d.z*tc_out.z);
  ivec3 tci3=ivec3(xNormPos, yNormPos, zNormPos);
  color=texelFetch(image_texture3d, tci3, 0);
}
