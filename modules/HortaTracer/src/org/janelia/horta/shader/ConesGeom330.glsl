#version 330
#extension GL_EXT_geometry_shader : enable

/**
 * Geometry shader for cone imposters.
 * Converts points at cone axis ends, into camera-facing bounding geometry
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


layout(lines) in; // two ends of cone
// Create viewer-facing imposter geometry 
layout(triangle_strip, max_vertices=12) out;


in float geomRadius[]; // radius at end of cone as vertex attribute


out float fragRadius; // average radius of cone
out vec3 center; // center of cone, in camera frame
out float taper; // change in radius per distance along cone axis
out vec3 halfAxis;
// the *linear* coefficients of the ray-tracing quadratic formula can be computed per-vertex, rather than per fragment.
out float tAP; // cone ray-casting quadratic-formula linear (actually constant) coefficient
out float qe_c; // cone ray-casting quadratic-formula linear coefficient
out float qe_half_b; // cone ray-casting quadratic-formula linear coefficient
out vec3 qe_undot_half_a; // cone ray-casting quadratic-formula linear coefficient
out float halfConeLength;
out vec3 aHat;
out vec3 imposterPos; // location of imposter bounding geometry, in camera frame
out float normalScale;
out float bViewAlongCone; // Is view angle less than taper angle?


// forward declaration of methods defined in imposter_fns330.glsl
void cone_linear_coeffs(in vec3 center, in float radius, in vec3 axis, in float taper, in vec3 pos,
        out float tAP, out float qe_c, out float qe_half_b, out vec3 qe_undot_half_a);



// Relative locations of all eight corners of the bounding prism (see diagram below)
const vec3 p1 = vec3(+1,+1,+1); // corner oriented toward viewer
const vec3 p2 = vec3(-1,+1,-1); // upper rear corner
const vec3 p3 = vec3(-1,+1,+1); // upper left corner
const vec3 p4 = vec3(-1,-1,+1); // lower left corner
const vec3 p5 = vec3(+1,-1,+1); // lower rear corner
const vec3 p6 = vec3(+1,-1,-1); // lower right corner
const vec3 p7 = vec3(+1,+1,-1); // upper right corner
const vec3 p8 = vec3(-1, -1, -1); // rear back corner

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
    imposterPos = center + offset;
    gl_Position = projectionMatrix * vec4(imposterPos, 1);

    cone_linear_coeffs(center, fragRadius, halfAxis, taper, imposterPos, 
        tAP, qe_c, qe_half_b, qe_undot_half_a);

    EmitVertex();
}



// sometimes you can see five of the six hull sides
void near_cone_hull(mat3 frame2348, mat3 frame1567) {
    emit_one_vertex(frame1567*p6);
    emit_one_vertex(frame1567*p7);
    emit_one_vertex(frame1567*p5);
    emit_one_vertex(frame1567*p1);
    emit_one_vertex(frame2348*p3);
    emit_one_vertex(frame1567*p7);
    emit_one_vertex(frame2348*p2);
    emit_one_vertex(frame1567*p6);
    emit_one_vertex(frame2348*p8);
    emit_one_vertex(frame1567*p5);
    emit_one_vertex(frame2348*p4);
    emit_one_vertex(frame2348*p3);

    EndPrimitive();
}

// sometimes you can see five of the six hull sides
void far_cone_hull(mat3 frame2348, mat3 frame1567) {
    emit_one_vertex(frame2348*p8);
    emit_one_vertex(frame2348*p2);
    emit_one_vertex(frame2348*p4);
    emit_one_vertex(frame2348*p3);
    emit_one_vertex(frame1567*p1);
    emit_one_vertex(frame2348*p2);
    emit_one_vertex(frame1567*p7);
    emit_one_vertex(frame2348*p8);
    emit_one_vertex(frame1567*p6);
    emit_one_vertex(frame2348*p4);
    emit_one_vertex(frame1567*p5);
    emit_one_vertex(frame1567*p1);

    EndPrimitive();
}

void main() {
    // On Mac GL_EXT_geometry_shader4 is unrecognized, so must use later geometry shader syntax
#ifdef GL_EXT_geometry_shader4
    vec4 posIn0 = gl_PositionIn[0]; // extension syntax
    vec4 posIn1 = gl_PositionIn[1]; // extension syntax
#else
    vec4 posIn0 = gl_in[0].gl_Position; // modern geometry shader syntax
    vec4 posIn1 = gl_in[1].gl_Position; // modern geometry shader syntax
#endif
    vec3 c1 = posIn0.xyz/posIn0.w; // center of smaller cone end
    vec3 c2 = posIn1.xyz/posIn1.w; // center of larger cone end
    float r1 = geomRadius[0];
    float r2 = geomRadius[1];

    // To make cones line up perfectly with the spheres, the ends
    // and radii need to be changed. Either on the CPU,
    // or here, in the shader on the GPU. Doing so on the GPU could
    // allow better dynamic changes to the radii.
    // This is a subtle effect that mainly affects cones 
    // connecting spheres of very different radii.
    const bool postModifyRadii = true;
    if (postModifyRadii) {
        // Modify locations and radii, so cone is flush with adjacent spheres
        vec3 cs1 = c1; // center of first sphere
        vec3 cs2 = c2; // center of second sphere
        float rs1 = r1;
        float rs2 = r2;
        // Swap so r2 is always the largest
        if (rs2 < rs1) {
            cs2 = c1;
            cs1 = c2;
            rs2 = r1;
            rs1 = r2;
        }
        float d = length(cs2 - cs1); // distance between sphere centers
        // half cone angle, to just touch each sphere
        float sinAlpha = (rs2 - rs1) / d;
        float cosAlpha = sqrt(1 - sinAlpha*sinAlpha);

        // Actual cone terminal radii might be smaller than sphere radii
        r1 = cosAlpha * rs1;
        r2 = cosAlpha * rs2;
        // Cone termini might not lie at sphere centers
        vec3 aHat0 = (cs1 - cs2)/d;
        vec3 dC1 = aHat0 * sinAlpha * rs1;
        vec3 dC2 = aHat0 * sinAlpha * rs2;
        // Cone termini
        c1 = cs1 + dC1;
        c2 = cs2 + dC2;
    }

    center = mix(c1, c2, 0.5); // centroid of cone
    vec3 cone_spine = c2 - c1;
    halfAxis = -0.5 * cone_spine;
    float cone_length = length(cone_spine);
    fragRadius = mix(r1, r2, 0.5); // radius at cone center
    taper = (r2 - r1) / cone_length;
    halfConeLength = 0.5 * cone_length;
    aHat = -cone_spine/cone_length;
    normalScale = 1.0 / sqrt(1.0 + taper*taper);

    // Decide whether view direction is sort of "along" cone axis, or 
    // sort of perpendicular to cone axis. Each case has different ray 
    // casting consequences.
    bViewAlongCone = 0; // default to false
    if (abs(taper) > 1e-4) { // not for cylinders...
        float cos_cone_angle = abs(cos(atan(r2 - r1, cone_length)));
        vec3 cone_tip = center + aHat * fragRadius/taper;
        float cos_view_angle = abs(dot(normalize(cone_tip), aHat));
        if (cos_view_angle > cos_cone_angle) bViewAlongCone = 1; // true
    }

    // Compute local coordinate system of cone bounding box
    // Put "X" axis of bounding geometry along cone axis
    vec3 x = cone_spine / cone_length;
    // ensure X axis points generally toward viewer
    if (dot(x, center) > 0) {
        x = -x; // point in opposite direction
        // swap radii
        float tmp = r1;
        r1 = r2;
        r2 = r1;
    }
    // To minimize overdraw, y should point out of the screen
    vec3 in_screen = cross(x, center);
    vec3 y = normalize(cross(x, in_screen));
    vec3 z = normalize(cross(x, y));
    // Ensure coordinate axes are a) right handed and b) out of the screen
    if (dot(z, center) > 0) z = -z;
    if (dot(y, center) > 0) y = -y;
    if (dot( cross(x, y), z) < 0) {
        // swap y and z
        vec3 temp = y;
        y = z;
        z = temp;
    }

    // Linear matrices to convert cube corner offsets to cone bounding prism
    // smaller-x, transform for cube points p2, p3, p4, and p8.
    mat3 frame2348 = mat3(
            0.5 * x * cone_length, 
            y * r1, 
            z * r1);
    // larger-x, transform for cube points p1, p5, p6, and p7.
    mat3 frame1567 = mat3(
            0.5 * x * cone_length, 
            y * r2, 
            z * r2);

    far_cone_hull(frame2348, frame1567); // near cone hull
}
