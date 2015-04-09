#version 330

uniform mat4 modelViewMatrix = mat4(1);
uniform mat4 projectionMatrix = mat4(1);
uniform float particleScale = 1000.0;

in vec3 position;
in float radius;

out vec4 frontPos; // for depth estimation
out vec4 midPos; // for depth estimation

void main() {
    vec4 eyePos = modelViewMatrix * vec4(position, 1);
    midPos = projectionMatrix * eyePos;
    gl_Position = midPos;
    frontPos = projectionMatrix * (eyePos + vec4(0,0,radius,0));
    gl_PointSize =  2.0 * radius * particleScale / abs(eyePos.z);

    // for debugging
    // gl_PointSize = 100;
    // gl_Position = vec4(0.5, 0.5, 0.5, 1.0);
}
