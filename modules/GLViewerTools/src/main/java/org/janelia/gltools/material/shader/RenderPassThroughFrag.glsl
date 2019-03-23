#version 330

uniform sampler2D upstreamImage;

in vec2 screenCoord; // from RenderPassVrtx shader

out vec4 fragColor;

void main() {
    vec4 c = texture(upstreamImage, screenCoord);
    fragColor = c;
}
