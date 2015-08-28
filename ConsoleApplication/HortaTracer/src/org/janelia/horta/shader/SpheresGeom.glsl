#version 330
#extension GL_EXT_geometry_shader : enable

/**
 * Geometry shader for sphere imposters.
 * Eventually, this will convert points into camera-facing bounding geometry
 */

uniform mat4 projectionMatrix = mat4(1);


layout(points) in;
// Create viewer-facing half-cube imposter geometry 
layout(triangle_strip, max_vertices=10) out;


in float geomRadius[];


// Rotate cube of size 2, so that corner 1,1,1 points toward +Z
// First rotate -45 degrees about Y axis, to orient +XZ edge toward +Z
// These values are all "const", so they are computed once at shader compile time.
// Y-axis is DOWN in mouse brain space, due to Fiji image convention
// But I'm thinking of this cube in Y-axis UP orientation
const float cos_45 = sqrt(2)/2;
const float sin_45 = sqrt(2)/2;
const mat3 rotY45 = mat3(
    cos_45, 0, sin_45,
    0,   1,   0,
    -sin_45, 0, cos_45);
// Next rotate by arcsin(1/sqrt(3)) about X-axis, to orient corner +XYZ toward +Z
const float sin_foo = -1.0/sqrt(3);
const float cos_foo = sqrt(2)/sqrt(3);
const mat3 rotXfoo = mat3(
    1,   0,   0,
    0, cos_foo, -sin_foo,
    0, sin_foo, cos_foo);
const mat3 identity = mat3(
    1, 0, 0,
    0, 1, 0,
    0, 0, 1);
const mat3 rotCorner = rotXfoo * rotY45; // identity; // rotXfoo * rotYm45;
const vec4 p1 = vec4(rotCorner * vec3(+1,+1,+1), 0); // corner oriented toward viewer
const vec4 p2 = vec4(rotCorner * vec3(-1,+1,-1), 0); // upper rear corner
const vec4 p3 = vec4(rotCorner * vec3(-1,+1,+1), 0); // upper left corner
const vec4 p4 = vec4(rotCorner * vec3(-1,-1,+1), 0); // upper left corner


void main() {
    vec4 center4 = gl_PositionIn[0];
    vec4 center = vec4(center4.xyz/center4.w, 1);
    float radius = geomRadius[0];

    gl_Position = projectionMatrix * (center + radius*p2);
    EmitVertex();

    gl_Position = projectionMatrix * (center + radius*p3);
    EmitVertex();

    gl_Position = projectionMatrix * (center + radius*p1);
    EmitVertex();

    gl_Position = projectionMatrix * (center + radius*p4);
    EmitVertex();

    EndPrimitive();
}
