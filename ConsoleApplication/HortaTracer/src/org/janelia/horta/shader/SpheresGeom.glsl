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


out float fragRadius;
out vec3 center;
out float c2; // sphere linear coefficient cee-squared
out float pc; // sphere linear coefficient pos-dot-center
out vec3 imposterPos;


// Rotate cube of size 2, so that corner 1,1,1 points toward +Z
// First rotate -45 degrees about Y axis, to orient +XZ edge toward +Z
// These values are all "const", so they are computed once at shader compile time.
// Y-axis is DOWN in mouse brain space, due to Fiji image convention
// But I'm thinking of this cube in Y-axis UP orientation.
// ...which is stupid, but I just flipped signs until it looked right.
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
const vec3 p1 = rotCorner * vec3(+1,+1,+1); // corner oriented toward viewer
const vec3 p2 = rotCorner * vec3(-1,+1,-1); // upper rear corner
const vec3 p3 = rotCorner * vec3(-1,+1,+1); // upper left corner
const vec3 p4 = rotCorner * vec3(-1,-1,+1); // lower left corner
const vec3 p5 = rotCorner * vec3(+1,-1,+1); // lower rear corner
const vec3 p6 = rotCorner * vec3(+1,-1,-1); // lower right corner
const vec3 p7 = rotCorner * vec3(+1,+1,-1); // upper right corner

/***
      2___________7                  
      /|         /|
     / |        / |                Y
   3/_________1/  |                ^
    |  |_______|__|6               |
    |  /       |  /                |
    | /        | /                 /---->X
    |/_________|/                 /
    4          5                 /
                                Z
*/


void main() {
    vec4 center4a = gl_PositionIn[0];
    center = center4a.xyz/center4a.w;
    float radius = geomRadius[0];
    fragRadius = radius;
    c2 = dot(center, center) - radius*radius; // same for all points

    // Half cube can be constructed using 2 triangle strips,
    // each with 3 triangles

    // First strip: 2-3-1-4-5
    imposterPos = center + radius*p2;
    gl_Position = projectionMatrix * vec4(imposterPos, 1);
    pc = dot(imposterPos, center);
    EmitVertex();

    imposterPos = center + radius*p3;
    gl_Position = projectionMatrix * vec4(imposterPos, 1);
    pc = dot(imposterPos, center);
    EmitVertex();

    imposterPos = center + radius*p1;
    gl_Position = projectionMatrix * vec4(imposterPos, 1);
    pc = dot(imposterPos, center);
    EmitVertex();

    imposterPos = center + radius*p4;
    gl_Position = projectionMatrix * vec4(imposterPos, 1);
    pc = dot(imposterPos, center);
    EmitVertex();

    imposterPos = center + radius*p5;
    gl_Position = projectionMatrix * vec4(imposterPos, 1);
    pc = dot(imposterPos, center);
    EmitVertex();

    EndPrimitive();

    // Second strip: 5-6-1-7-2
    imposterPos = center + radius*p5;
    gl_Position = projectionMatrix * vec4(imposterPos, 1);
    pc = dot(imposterPos, center);
    EmitVertex();

    imposterPos = center + radius*p6;
    gl_Position = projectionMatrix * vec4(imposterPos, 1);
    pc = dot(imposterPos, center);
    EmitVertex();

    imposterPos = center + radius*p1;
    gl_Position = projectionMatrix * vec4(imposterPos, 1);
    pc = dot(imposterPos, center);
    EmitVertex();

    imposterPos = center + radius*p7;
    gl_Position = projectionMatrix * vec4(imposterPos, 1);
    pc = dot(imposterPos, center);
    EmitVertex();

    imposterPos = center + radius*p2;
    gl_Position = projectionMatrix * vec4(imposterPos, 1);
    pc = dot(imposterPos, center);
    EmitVertex();

    EndPrimitive();

 }
