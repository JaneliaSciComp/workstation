#version 330
#extension GL_EXT_geometry_shader : enable

/**
 * Geometry shader for cone imposters.
 * Converts points at cone axis ends, into camera-facing bounding geometry
 * NOTE - for SWC representation, this shader assumes that positions and radii have
 * already been shift to be flush with adjacent sphere geometry.
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

out vec3 imposterPos; // location of imposter bounding geometry, in camera frame


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
void cone_hull(mat3 frame2348, mat3 frame1567) {
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


void main() {
    vec3 c1 = gl_PositionIn[0].xyz/gl_PositionIn[0].w; // center of smaller cone end
    vec3 c2 = gl_PositionIn[1].xyz/gl_PositionIn[1].w; // center of larger cone end
    center = mix(c1, c2, 0.5); // centroid of cone
    vec3 cone_spine = c2 - c1;
    halfAxis = -0.5 * cone_spine;
    float cone_length = length(cone_spine);
    fragRadius = mix(geomRadius[0], geomRadius[1], 0.5); // radius at cone center
    taper = (geomRadius[1] - geomRadius[0]) / cone_length;

    // Compute local coordinate system of cone bounding box
    // Put "X" axis of bounding geometry along cone axis
    vec3 x = cone_spine / cone_length;
    bool invertedX = false;
    // ensure X axis points generally toward viewer
    float r1 = geomRadius[0];
    float r2 = geomRadius[1];
    if (dot(x, center) > 0) {
        invertedX = true;
        x = -x; // point in opposite direction
        r1 = geomRadius[1];
        r2 = geomRadius[0];
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
    // Low-x, smaller radius transform for cube points p2, p3, p4, and p8.
    mat3 frame2348 = mat3(
            0.5 * x * cone_length, 
            y * r1, 
            z * r1);
    mat3 frame1567 = mat3(
            0.5 * x * cone_length, 
            y * r2, 
            z * r2);

    cone_hull(frame2348, frame1567); // simpler geometry, imposter intersects cone (6 vertices)
}
