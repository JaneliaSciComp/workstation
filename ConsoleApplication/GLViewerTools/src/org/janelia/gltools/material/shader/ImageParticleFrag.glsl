#version 330

uniform sampler2D sphereTexture;
uniform vec4 specularColor = vec4(0.1, 1.0, 0.1, 1);
uniform vec4 diffuseColor  = vec4(0.1, 1.0, 0.1, 1);

in vec4 frontPos; // for depth estimation
in vec4 midPos; // for depth estimation

out vec4 fragColor;

void main() {
    // fragColor = vec4(0, 1, 0, 1); return;

    vec2 uv = vec2(gl_PointCoord.x, gl_PointCoord.y);
    vec4 sphereColors = texture(sphereTexture, uv);

    // avoid further computation at invisible corners
    if (sphereColors.a < 0.5) discard;

    // diffuse color in red channel
    fragColor = diffuseColor * sphereColors.r;
    fragColor += specularColor * sphereColors.g;

    // TODO - depth
}
