#version 330

/**
 * OpenGL shading language shader
 */

uniform sampler3D volumeTexture;

in vec3 fragTexCoord;

out vec4 fragData;

void main() {
    vec4 c = texture(volumeTexture, fragTexCoord);
    fragData = vec4(c.rrrr); // intensity/alpha in red channel
}
