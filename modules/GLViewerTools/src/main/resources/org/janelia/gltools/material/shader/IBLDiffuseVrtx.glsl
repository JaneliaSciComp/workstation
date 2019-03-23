#version 330

// Diffuse component of Image Based Lighting proof-of-concept

uniform mat4 modelViewMatrix = mat4(1);
uniform mat4 projectionMatrix = mat4(1); 

in vec3 position;
in vec3 normal;

out vec3 fragNormal;
out vec4 fragPosition;

void main() {
    fragPosition = modelViewMatrix * vec4(position, 1);
    gl_Position = projectionMatrix * fragPosition;
    // TODO - for more generality, use the NormalMatrix
    fragNormal = (modelViewMatrix * vec4(normal, 0)).xyz;
}
