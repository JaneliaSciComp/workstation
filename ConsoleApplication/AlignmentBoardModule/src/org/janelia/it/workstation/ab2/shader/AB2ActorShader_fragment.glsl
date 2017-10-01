#version 410

in vec4 vColor;
flat in int ait;
out vec4 color;

uniform usampler2D image_texture;

void main() {
  if (ait==0) {
    color = vColor;
  } else {
    ivec2 frag_coord=ivec2(gl_FragCoord.xy);
    color=texelFetch(image_texture, frag_coord, 0);
  }
}
