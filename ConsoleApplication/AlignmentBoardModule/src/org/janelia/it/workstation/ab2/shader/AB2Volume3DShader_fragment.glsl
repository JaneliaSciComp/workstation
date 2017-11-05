#version 450

in vec3 tc_out;
out vec4 color;

uniform vec3 image_dim;
uniform float image_max_dim;

uniform sampler2DArray image_texture3d;

void main() {
  //ivec3 imageSize3d=textureSize(image_texture3d, 0);
  float midMax=image_max_dim/2.0;
  vec3 image_dim2=image_dim/2.0;
  float xNormPos=image_dim2.x + (tc_out.x*image_max_dim-midMax);
  float yNormPos=image_dim2.y + (tc_out.y*image_max_dim-midMax);
  float zNormPos=image_dim2.z + (tc_out.z*image_max_dim-midMax);

  //ivec3 tci3=ivec3(imageSize3d.x*tc_out.x, imageSize3d.y*tc_out.y, imageSize3d.z*tc_out.z);
  if (xNormPos<0.0 || yNormPos<0.0 || zNormPos<0.0) {
     discard;
  } else {
     ivec3 tci3=ivec3(xNormPos, yNormPos, zNormPos);
     if (tci3.x>image_dim.x || tci3.y>image_dim.y || tci3.z>image_dim.z) {
        discard;
     } else {
        color=texelFetch(image_texture3d, tci3, 0);
        if (color.r<0.05 && color.g<0.05 && color.b<0.05) {
           discard;
        }
     }
  }
}
