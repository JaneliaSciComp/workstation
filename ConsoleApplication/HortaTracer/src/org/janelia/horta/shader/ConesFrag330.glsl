#version 330

/**
 * Sphere imposter fragment shader.
 */

uniform vec4 color = vec4(0, 1, 0, 1); // one color for all spheres
uniform mat4 projectionMatrix; // needed for proper sphere depth calculation
uniform sampler2D lightProbe;


in vec3 imposterPos; // imposter geometry location, in camera frame
in vec3 center; // sphere center in camera frame
in float fragRadius; // sphere radius


out vec4 fragColor;


// forward declaraion of methods defined in imposter_fns330.glsl
// TODO:


void main() {
    // TODO: proper fragment shader here...
    fragColor = vec4(0, 0, 1, 1);
}
