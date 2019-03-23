#version 330

/**
 * Experimental shader to render equirectangular projection.
 * Client code is responsible for ensuring that window aspect ratio is 2:1,
 * for canonical equirectangular rendering.
 */

const float one_pi = 3.14159265358;
const float two_pi = 2.0 * one_pi;
const float half_pi = 0.5 * one_pi;

uniform mat4 modelViewMatrix = mat4(1);
uniform mat4 projectionMatrix = mat4(1);

in vec3 position;
in vec3 texCoord;

out vec3 fragTexCoord;

// Convert vertex coordinates from camera space to projected equirectangular space,
// Using radial zNear/zFar from a more traditional perspective projection matrix
vec4 equirect_from_persp(in vec4 pos_in_camera, in mat4 perspMat) 
{
    vec3 p = pos_in_camera.xyz/pos_in_camera.w; // vertex location in camera space

    // Compute latitude/longitude of vertex in camera space for equirectangular projection
    float longitude = atan(p.x, -p.z) / one_pi; // in ndc space, note positive Z is behind you in OpenGL
    float xzradius = length(p.xz); // in world units
    float latitude = atan(p.y, xzradius) / half_pi; // in ndc space
    float radius = length(p); // Distance to camera, analog of perspective "-z" divisor
    
    // Instead of using projection matrix, project equirectangular projection
    return vec4(
        longitude * radius,
        latitude * radius,
        (perspMat * vec4(0, 0, -radius, 1)).z, // delegate depth calculation to perspective matrix
        radius);
}

void main() {
    vec4 pos_c = modelViewMatrix * vec4(position, 1);
    
    // Instead of using projection matrix, project equirectangular projection
    gl_Position = equirect_from_persp(pos_c, projectionMatrix);

    // gl_Position = projectionMatrix * pos_c0; // traditional perspective projection

    fragTexCoord = texCoord;
}
