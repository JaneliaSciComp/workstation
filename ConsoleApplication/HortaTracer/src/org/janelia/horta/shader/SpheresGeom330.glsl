#version 330
#extension GL_EXT_geometry_shader4 : enable

/**
 * Geometry shader for sphere imposters.
 * Converts points at sphere center, into camera-facing bounding geometry
 */

/* 
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

uniform mat4 projectionMatrix = mat4(1);


layout(points) in; // input vertices are sphere centers
// Create viewer-facing half-cube imposter geometry 
// layout(triangle_strip, max_vertices=10) out;
layout(triangle_strip, max_vertices=10) out; // only six vertices needed for "mid-hull" approach


in float geomRadius[]; // sphere radius as vertex attribute


out float fragRadius; // pass radius of sphere to fragment shader
out vec3 center; // center of sphere, in camera frame
// the *linear* coefficients of the ray-tracing quadratic formula can be computed per-vertex, rather than per fragment.
out float c2; // sphere ray-casting quadratic-formula linear (actually constant) coefficient cee-squared
out float pc; // sphere ray-casting quadratic-formula linear coefficient pos-dot-center
out vec3 imposterPos; // location of imposter bounding geometry, in camera frame


// Create a bounding cube, with one corner oriented toward the viewer.
// Rotate cube of size 2, so that corner 1,1,1 points toward +Z
// First rotate -45 degrees about Y axis, to orient +XZ edge toward +Z
// These values are all "const", so they are computed once at shader compile time, not during shader execution.
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
// Relative locations of all eight corners of the cube (see diagram below)
const vec3 p1 = rotCorner * vec3(+1,+1,+1); // corner oriented toward viewer
const vec3 p2 = rotCorner * vec3(-1,+1,-1); // upper rear corner
const vec3 p3 = rotCorner * vec3(-1,+1,+1); // upper left corner
const vec3 p4 = rotCorner * vec3(-1,-1,+1); // lower left corner
const vec3 p5 = rotCorner * vec3(+1,-1,+1); // lower rear corner
const vec3 p6 = rotCorner * vec3(+1,-1,-1); // lower right corner
const vec3 p7 = rotCorner * vec3(+1,+1,-1); // upper right corner
const vec3 p8 = rotCorner * vec3(-1, -1, -1); // rear back corner

/*
      2___________7                  
      /|         /|
     / |        / |                Y
   3/_________1/  |                ^
    | 8|_______|__|6               |
    |  /       |  /                |
    | /        | /                 /---->X
    |/_________|/                 /
    4          5                 /
                                Z
*/


void emit_one_vertex(vec3 offset) {
    // Because we always view the cube on-corner, we can afford to trim the bounding geometry a bit
    const float trim = 0.72; // 0.70 = aggressive trim, determined empirically
    imposterPos = center + trim * geomRadius[0] * offset;
    gl_Position = projectionMatrix * vec4(imposterPos, 1);
    pc = dot(imposterPos, center);
    EmitVertex();
}


// half-cube imposter hull that is closer to viewer than actual sphere
// (this property might be useful for clever early-depth-test optimization)
// (position of hull also affects pattern of appearance/disappearance at near and far clip planes.)
void near_hull() {
    // Half cube can be constructed using 2 triangle strips,
    // each with 3 triangles
    // First strip: 2-3-1-4-5
    emit_one_vertex(p2);
    emit_one_vertex(p3);
    emit_one_vertex(p1);
    emit_one_vertex(p4);
    emit_one_vertex(p5);
    EndPrimitive();
    // Second strip: 5-6-1-7-2
    emit_one_vertex(p5);
    emit_one_vertex(p6);
    emit_one_vertex(p1);
    emit_one_vertex(p7);
    emit_one_vertex(p2);
    EndPrimitive();
}


// half-cube imposter hull that is farther from viewer than actual sphere
// (this property might be useful for clever early-depth-test optimization)
// (position of hull also affects pattern of appearance/disappearance at near and far clip planes.)
// The far hull differs from the near hull only in replacing p1 with p8.
void far_hull() {
    // Half cube can be constructed using 2 triangle strips,
    // each with 3 triangles
    // First strip: 2-3-8-4-5
    emit_one_vertex(p2);
    emit_one_vertex(p3);
    emit_one_vertex(p8);
    emit_one_vertex(p4);
    emit_one_vertex(p5);
    EndPrimitive();
    // Second strip: 5-6-8-7-2
    emit_one_vertex(p5);
    emit_one_vertex(p6);
    emit_one_vertex(p8);
    emit_one_vertex(p7);
    emit_one_vertex(p2);
    EndPrimitive();
}

// Simplified imposter geometry using only edge points, i.e. ignoring near/far corner.
// Uses only 4 triangles and 1 triangle strip
// Intersects sphere roughly near center plane, but sort of undulating triangles,
// like a cyclohexane "chair" configuration, if you know what that is...
void mid_hull() {
    emit_one_vertex(p3);
    emit_one_vertex(p4);
    emit_one_vertex(p2);
    emit_one_vertex(p5);
    emit_one_vertex(p7);
    emit_one_vertex(p6);
    EndPrimitive();
}


void main() {
    // On Mac GL_EXT_geometry_shader4 is unrecognized, so must use later geometry shader syntax
#ifdef GL_EXT_geometry_shader4
    vec4 posIn = gl_PositionIn[0]; // extension syntax
#else
    vec4 posIn = gl_in[0].gl_Position; // modern geometry shader syntax
#endif
    center = posIn.xyz/posIn.w; // sphere center is constant for all vertices
    fragRadius = geomRadius[0]; // sphere radius is constant for all vertices
    c2 = dot(center, center) - fragRadius*fragRadius; // 2*c coefficient is constant for all vertices

    // Choice of imposter hull strategies below
    // NOTE: modify the above "layout(..., max_vertices=...) out;" statement to match your chosen hull strategy
    // near_hull(); // imposter in front of sphere (10 vertices)
    far_hull(); // imposter behind sphere (10 vertices)
    // mid_hull(); // simpler geometry, imposter intersects sphere (6 vertices)
 }
