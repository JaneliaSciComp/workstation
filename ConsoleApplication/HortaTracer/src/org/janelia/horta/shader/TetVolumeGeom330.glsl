/*
 * Tetrahedral volume rendering geometry shader 
 * to attach vertex information needed to find the front face entry point.
 */

#version 330 core
#extension GL_EXT_geometry_shader4 : enable

// Project here in the geometry shader
uniform mat4 projectionMatrix = mat4(1);

uniform int renderPass = 1; // 1: front tetrahedra, 2: central tetrahedra, 3: rear tetrahedra

/* Receive one tetrahedral mesh as a base triangle, plus three redundant side vertices
   The base of the tetrahedron is triangle 0-4-2, below.
   Vertices 1, 3, and 5 are all the same fourth apex vertex of the tetrahedron.

   six-vertex                      four-vertex
   triangle adjacency:             tetrahedron:

         1---2---3                       2(1)
          \  |\  |                      /|\
           \ | \ |                     / | \ (base)
            \|  \|                    /  |  \
             0---4                   0---|---4(2)
              \  |                    \  |  /
               \ |                     \ | /
                \|                      \|/
                 5                    1/3/5(3) (apex)         */
layout(triangles_adjacency) in;

// Emit back-facing triangles.
// When the viewpoint is inside the vertex, we might need to emit all four triangles,
// for a total of twelve vertices.
layout(triangle_strip, max_vertices=12) out; 

in vec3 geomTexCoord[]; // 3D intensity texture coordinate for volume rendering

// Matrix to convert camera coordinates to (first three) barycentric coordinates
// out mat4 baryFromCamera;

out vec4 barycentricCoord;
out vec3 fragTexCoord;

// void emit_triangle(in vec4 p1, in vec4 p2, in vec4 p3) 
void emit_triangle(in vec4[4] v, in vec4[4] b, in vec3[4] t, in int p1, in int p2, in int p3) 
{
    // TODO: reject front-facing triangles
    gl_Position = v[p1];
    barycentricCoord = b[p1];
    fragTexCoord = t[p1];
    EmitVertex();
    gl_Position = v[p2];
    barycentricCoord = b[p2];
    fragTexCoord = t[p2];
    EmitVertex();
    gl_Position = v[p3];
    barycentricCoord = b[p3];
    fragTexCoord = t[p3];
    EmitVertex();
    EndPrimitive();
}

// Detect front faces
// Answers the question: Is the camera-space triangle ABC facing the camera?
bool isFront(vec3 a, vec3 b, vec3 c)
{
    return dot(cross(a-b, c-b), b) > 0;
}
bool isFront(vec4 A, vec4 B, vec4 C)
{
    // Convert homogenous coordinates to regular 3D
    return isFront(A.xyz/A.w, B.xyz/B.w, C.xyz/C.w); 
}

void main() {
    // We only need to project the 4 unique points, not all six.
    vec4 projected[4] = vec4[4] (
        projectionMatrix * gl_in[0].gl_Position, // base1
        projectionMatrix * gl_in[2].gl_Position, // base2
        projectionMatrix * gl_in[4].gl_Position, // base3
        projectionMatrix * gl_in[1].gl_Position); // apex

    // A front-facing tetrahedron has a NON-front-facing base triangle...
    // TODO: this is not working perfectly yet...
    bool front = ! isFront(gl_in[0].gl_Position, gl_in[2].gl_Position, gl_in[4].gl_Position);
    if ((renderPass == 1) && (! front))
        return; // don't draw this tetrahedron on this pass
    else if ((renderPass == 3) && front)
        return; // don't draw this tetrahedron on this pass

    vec3 tc[4] = vec3[4] (
        geomTexCoord[0],
        geomTexCoord[2],
        geomTexCoord[4],
        geomTexCoord[1]);
    const vec4 bary[4] = vec4[4] (
        vec4(1, 0, 0, 0), // base1
        vec4(0, 1, 0, 0), // base2
        vec4(0, 0, 1, 0), // base3
        vec4(0, 0, 0, 1)); // apex

    // TODO: show back faces, and color by exit texture coordinates
    emit_triangle(projected, bary, tc, 0, 1, 2); // base triangle of tetrahedron
    emit_triangle(projected, bary, tc, 1, 0, 3);
    emit_triangle(projected, bary, tc, 0, 2, 3);
    emit_triangle(projected, bary, tc, 2, 1, 3);
}
