#version 410

in vec4 vColor0;
in vec4 vColor1;
in vec3 tc_out;
flat in int textureTypeOut;
out vec4 color;

//  TEXTURE_TYPE_NONE    =0;
//  TEXTURE_TYPE_2D_RGBA =1;
//  TEXTURE_TYPE_2D_R8   =2;
//  TEXTURE_TYPE_3D_RGBA =3;

uniform sampler2D image_texture;
uniform sampler3D image_texture3d;

void main() {

  ivec2 imageSize2d;
  ivec3 imageSize3d;
  vec4 colorRgba;
  ivec2 tci2;
  ivec3 tci3;

  switch(textureTypeOut) {
    case 0:
      color=vColor0;
      break;
    case 1:
      imageSize2d=textureSize(image_texture, 0);
      tci2=ivec2(imageSize2d.x*tc_out.x, imageSize2d.y*tc_out.y);
      color=texelFetch(image_texture, tci2, 0);
      break;
    case 2:
      ivec2 imageSize2d=textureSize(image_texture, 0);
      tci2=ivec2(imageSize2d.x*tc_out.x, imageSize2d.y*tc_out.y);
      colorRgba=texelFetch(image_texture, tci2, 0);
      color=mix(vColor1, vColor0, colorRgba.r);
      break;
    case 3:
      ivec3 imageSize3d=textureSize(image_texture3d, 0);
      tci3=ivec3(imageSize3d.x*tc_out.x, imageSize3d.y*tc_out.y, imageSize3d.z*tc_out.z);
      color=texelFetch(image_texture3d, tci3, 0);
      //color=vec4(0f, 1f, 0f, 1f);
      // DEBUG
      //if (c.r>0.05 || c.g>0.05 || c.b>0.05) {
      //  color=vec4(0f, 1f, 0f, 1f);
      //} else {
      //  color=vec4(0.05f, 0f, 0f, 0.05f);
      //}
      break;
    default:
      color=vColor0;
  }
}
