#version 330

/**
 * OpenGL shading language shader
 */

uniform mat4 modelViewMatrix = mat4(1);
uniform mat4 projectionMatrix = mat4(1);

in vec3 position;
in vec3 texCoord;

out vec3 fragTexCoord;

void main() {
    vec4 fragPosition = modelViewMatrix * vec4(position, 1);
    gl_Position = projectionMatrix * fragPosition;
    fragTexCoord = texCoord;
}
