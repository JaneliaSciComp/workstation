/*
 * Tetrahedral volume rendering geometry shader 
 * Attaches vertex information needed to find the front face entry point,
 * while actually emitting the back face geometry.
 */

#version 430 core

// We will project vertices here in the geometry shader
layout(location = 1) uniform mat4 projectionMatrix = mat4(1);

// explicitly specify "official" near and far clip planes, which might be 
// less generous than those used to render our bounding geometry.
layout(location = 2) uniform vec3 opaqueZNearFarFocus = vec3(1e-2, 1e4, 1.0);

/* Receive one tetrahedral mesh as a base triangle, plus three redundant side vertices
   The base of the tetrahedron is triangle 0-4-2, below.
   Vertex 1 is the fourth apex vertex of the tetrahedron.
   Vertices 3 & 5 are actually the same as vertices 4 & 2, but ordered so that the 
     orientation of the 0-3-5 triangle indicates whether this tetrahedron is
     currently near the front of the full 5-tetrahedron cube. This information
     is used to draw the tetrahedra in strict back-to-front order within the cube.

   six-vertex                      four-vertex
   triangle adjacency:             tetrahedron:

         1---2---3                       2
          \  |\  |                      /|\
           \ | \ |                     / | \ (base)
            \|  \|                    /  |  \
             0---4                   0---|---4
              \  |                    \  |  /
               \ |                     \ | /
                \|                      \|/
                 5                       1(3/5) (apex)         */
layout(triangles_adjacency) in;

// Emit back-facing triangles.
// When the viewpoint is inside the tetrahedron, we might need to emit all four triangles,
// for a total of twelve vertices.
layout(triangle_strip, max_vertices=12) out; 

in vec3 geomTexCoord[]; // array of 3D intensity texture-coordinates for volume rendering

out vec3 fragTexCoord; // vertex texture coordinate
flat out vec3 cameraPosInTexCoord; // location of observer view point, in units of texture coordinates
flat out mat4 tetPlanesInTexCoord; // plane equations for each face of the tetrahedron, for use in ray bound clipping
flat out vec4 zNearPlaneInTexCoord; // plane equation for near z-clip plane
flat out vec4 zFarPlaneInTexCoord; // plane equation for far z-clip plane
flat out vec4 zFocusPlaneInTexCoord; // plane equation for focus z-plane
flat out mat4 planeTransform; // converts plane equations in camera space to texture coordinate space


// pass one face of this tetrahedron down to the fragment shader
void emit_triangle(in vec4[4] v, in vec3[4] t, in int p1arg, in int p2arg, in int p3arg) 
{
    int p1 = p1arg;
    int p2 = p2arg;
    int p3 = p3arg;
    const bool useRearTriangles = true; // Definitely use rear triangles here
    if (useRearTriangles) {
        // Reject front-facing triangles
        // reverse sense of triangle, so GL_CULL_FACE(BACK) actually DRAWS these back triangles.
        p1 = p3arg;
        p3 = p1arg;
    }
    // TODO: maybe perform back face culling directly here, instead of relying on glCullFace(GL_BACK)...

    gl_Position = v[p1];
    fragTexCoord = t[p1];
    EmitVertex();
    gl_Position = v[p2];
    fragTexCoord = t[p2];
    EmitVertex();
    gl_Position = v[p3];
    fragTexCoord = t[p3];
    EmitVertex();
    EndPrimitive();
}

// Detect front faces
// Answers the question: Is the camera-space triangle ABC facing the camera?
bool isFront(vec3 a, vec3 b, vec3 c)
{
    return dot(cross(b-a, c-b), b) <= 0;
}
bool isFront(vec4 A, vec4 B, vec4 C)
{
    // Convert homogenous coordinates to regular 3D
    return isFront(A.xyz/A.w, B.xyz/B.w, C.xyz/C.w);
}

// Returns an implicit plane equation for the plane containing the oriented triangle abc
vec4 planeForTriangle(vec3 a, vec3 b, vec3 c) {
    vec3 normal = normalize(cross(b-a, c-b));
    float dist = dot(normal, b);
    return vec4(normal, -dist);
}

void main() 
{
    // Only draw "rear" tetrahedra. This way, over the course of three render
    // passes, the five tetrahedra comprising a block will be drawn in 
    // painter's algorithm order.
    // Abuse elements 3&5 to encode front-ness
    if (! isFront(gl_in[0].gl_Position, gl_in[3].gl_Position, gl_in[5].gl_Position))
        return;

    // We only need to project the 4 unique points, not all six.
    vec4 projected[4] = vec4[4] (
        projectionMatrix * gl_in[0].gl_Position, // base1
        projectionMatrix * gl_in[2].gl_Position, // base2
        projectionMatrix * gl_in[4].gl_Position, // base3
        projectionMatrix * gl_in[1].gl_Position); // apex
    vec3 tc[4] = vec3[4] (
        geomTexCoord[0],
        geomTexCoord[2],
        geomTexCoord[4],
        geomTexCoord[1]);

    // Our goal is to emit imposter geometry, consisting of only the back faces of the tetrahedron.
    
    // Need to compute camera position in texture coordinates for this tetrahedron.
    // So we need to compute a transform that maps camera-space to texCoord-space.
    // I think I can compose this from two transforms that map to an intermediate
    // space, partial barycentric coordinates.
    // TODO: This could be done in advance on the CPU side
    // First simplify camera-space corner coordinates
    vec3 p1 = gl_in[0].gl_Position.xyz / gl_in[0].gl_Position.w;
    vec3 p2 = gl_in[2].gl_Position.xyz / gl_in[2].gl_Position.w;
    vec3 p3 = gl_in[4].gl_Position.xyz / gl_in[4].gl_Position.w;
    vec3 p4 = gl_in[1].gl_Position.xyz / gl_in[1].gl_Position.w;
    mat4 cameraFromBary = mat4(
            vec4(p1 - p4, 0),
            vec4(p2 - p4, 0),
            vec4(p3 - p4, 0),
            vec4(p4, 1));
    vec3 t1 = geomTexCoord[0];
    vec3 t2 = geomTexCoord[2];
    vec3 t3 = geomTexCoord[4];
    vec3 t4 = geomTexCoord[1];
    mat4 texCoordFromBary = mat4(
            vec4(t1 - t4, 0),
            vec4(t2 - t4, 0),
            vec4(t3 - t4, 0),
            vec4(t4, 1));
    mat4 baryFromCamera = inverse(cameraFromBary);
    mat4 texCoordFromCamera = texCoordFromBary * baryFromCamera;
    vec4 camPosHomo = texCoordFromCamera * vec4(0, 0, 0, 1);
    cameraPosInTexCoord = camPosHomo.xyz/camPosHomo.w;

    // For ray-clipping, compute one inward facing plane equation for each face of the tetrahedron
    // Because we are clipping to the INSIDE of the tetrahedron, these
    // triangles wind in the opposite of the usual direction.
    tetPlanesInTexCoord = mat4(
        planeForTriangle(t1, t3, t2),
        planeForTriangle(t1, t4, t3),
        planeForTriangle(t1, t2, t4),
        planeForTriangle(t2, t3, t4));

    // precompute plane equation for near/far clip planes
    float zNear = opaqueZNearFarFocus[0];
    float zFar = opaqueZNearFarFocus[1];
    float zFocus = opaqueZNearFarFocus[2];
    // TODO: This plane convention seems to be opposite to VolumeMipMaterial
    vec4 zNearPlaneInCamera = vec4(0, 0, -1.0, -zNear);
    // http://www.cs.brandeis.edu/~cs155/Lecture_07_6.pdf
    planeTransform = inverse(texCoordFromCamera);
    zNearPlaneInTexCoord = zNearPlaneInCamera * planeTransform; // look it up...
    vec4 zFarPlaneInCamera = vec4(0, 0, 1.0, zFar);
    zFarPlaneInTexCoord = zFarPlaneInCamera * planeTransform; 
    vec4 zFocusPlaneInCamera = vec4(0, 0, 1.0, zFocus);
    zFocusPlaneInTexCoord = zFocusPlaneInCamera * planeTransform; 

    emit_triangle(projected, tc, 0, 1, 2); // base triangle of tetrahedron
    emit_triangle(projected, tc, 1, 0, 3);
    emit_triangle(projected, tc, 0, 2, 3);
    emit_triangle(projected, tc, 2, 1, 3);
}
