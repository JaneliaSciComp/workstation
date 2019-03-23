#version 330

// Outline/silhouette geometry shader.
// Displays an outline halo around the outer visible edge of polygonal geometry.
// The "lineWidth" uniform parameter specifies the outline width in scene 
// units. Thus nearer subjects have thicker outlines, and more distant subjects,
// thinner outlines.
// The "depthFalloff" parameter allows thinning of the outline for subjects
// that are close to the geometry behind them. A value of "0.0" causes no
// thinning. A value of "10.0" reduces the thickness of outlines for subjects
// less than 10.0 * lineWidth in front of the next furthest subjects.

uniform mat4 projectionMatrix = mat4(1);
uniform float lineWidth = 2.0;
// Outer edge of silhouette is depthFalloff lineWidths farther away than is near edge
uniform float depthFalloff = 20.0;

layout(triangles_adjacency) in;
layout(triangle_strip, max_vertices=18) out;

out float quad_y; // outer edge antialiasing parameter
out vec4 testColor; // debugging

// Increase depth of outer edge of outline, to thin outlines of subjects close
// to depth of deeper neighbors.
vec3 push_depth(vec3 p) {
    return p + normalize(p) * depthFalloff * lineWidth;
}

// Compute gap-filling positions, like points ts0 and ts5
vec3 fill_corner(vec3 viewDirection, vec3 p1, vec3 p2, vec3 p3) {
    // Point ts0
    // bisect angle 1-2-3
    vec3 result = normalize(p2 - p1) + normalize(p2 - p3);
    // project into same view plane as edge
    result = result - viewDirection * dot(viewDirection, result);
    // Scale to outline width
    result = lineWidth * normalize(result);
    result = p2 + result;
    return result;
}

// Method paintEdge(...) Display one segment of silhouette shape
// 
// INPUT: 4 points representing a front-facing line strip. The central two 
// points of the strip represent the edge to be outlined. The outer points
// are used to help extend the outline, to fill in gaps between edge outlines.
// So, the outer points should be chosen to represent the least convex front-
// facing line strip available, to minimize the duplication of overlap.
// edge[1-4]:
//
//      3------2
//     /        \
//    4          1
//
// OUTPUT:
// Emit 4-6 points, to form one triangle strip representing this outline edge
// These output points are called ts0 ... ts5 ("ts" for "triangle strip")
// ts[0-5]:
//
//    0--1-------3---5
//     \ |       |  /
//       2-------4
// 
// Points 0 and 5 need not be colinear with 1--3
// Points 0 and 5 add triangles to help fill in gaps between triangle outlines
// Linewidth distance == 3->4 == 1->2 == 4->5 == 0->2
void paintEdge(vec3 edge1, vec3 edge2, vec3 edge3, vec3 edge4) 
{
    vec3 fullEdge = edge3 - edge2;
    float edgeLength = length(fullEdge);

    // "y" axis is perpendicular to both edge and view directions,
    // and points inward, toward shape/triangle interior
    vec3 viewDirection = normalize(edge2 + edge3);
    vec3 thicknessDirection = lineWidth * normalize(cross(viewDirection, fullEdge)); // perpendicular to view

    vec3 ts2 = edge3;
    vec3 ts4 = edge2;
    vec3 ts3 = ts4 + thicknessDirection;
    vec3 ts1 = ts2 + thicknessDirection;

    testColor = vec4(1, 0, 0, 1);
    // Point ts0
    quad_y = 1.0;
    vec3 ts0 = fill_corner(viewDirection, edge2, edge3, edge4);
    gl_Position = projectionMatrix * vec4(push_depth(ts0), 1);
    EmitVertex();

    // Point ts1: outer edge end
    testColor = vec4(0, 0, 1, 1);
    quad_y = 1.0;
    gl_Position = projectionMatrix * vec4(push_depth(ts1), 1);
    EmitVertex();

    // Point ts2: inner edge end
    quad_y = 0.0;
    gl_Position = projectionMatrix * vec4(ts2, 1);
    EmitVertex();

    // Point ts3: outer edge begin
    quad_y = 1.0;
    gl_Position = projectionMatrix * vec4(push_depth(ts3), 1);
    EmitVertex();

    // Point ts4: inner edge begin
    quad_y = 0.0;
    gl_Position = projectionMatrix * vec4(ts4, 1);
    EmitVertex();

    testColor = vec4(0, 1, 0, 1);
    // point ts5
    quad_y = 1.0;
    vec3 ts5 = fill_corner(viewDirection, edge1, edge2, edge3);
    gl_Position = projectionMatrix * vec4(push_depth(ts5), 1);
    EmitVertex();

    EndPrimitive();
}

// Determine whether a particular triangle faces the camera
bool isFrontFacing(vec3 A, vec3 B, vec3 C)
{
    vec3 faceNormal = cross(B - A, C - A);
    vec3 faceCenter = (A + B + C) * 1.0/3.0;
    return dot(faceNormal, faceCenter) <= 0;
}

void main() 
{
    // Center triangle must be front facing, or else we paint nothing
    if (! isFrontFacing(
            gl_in[0].gl_Position.xyz, 
            gl_in[2].gl_Position.xyz, 
            gl_in[4].gl_Position.xyz)) 
    {
        return;
    }

    bool isFront012 = isFrontFacing(
            gl_in[0].gl_Position.xyz, 
            gl_in[1].gl_Position.xyz, 
            gl_in[2].gl_Position.xyz);
    bool isFront234 = isFrontFacing(
            gl_in[2].gl_Position.xyz, 
            gl_in[3].gl_Position.xyz, 
            gl_in[4].gl_Position.xyz);
    bool isFront450 = isFrontFacing(
            gl_in[4].gl_Position.xyz, 
            gl_in[5].gl_Position.xyz, 
            gl_in[0].gl_Position.xyz);

    // But neighbor triangles must not face front, to get drawn
    if (! isFront012) 
    {
        paintEdge(
                isFront450 ? gl_in[5].gl_Position.xyz : gl_in[4].gl_Position.xyz,
                gl_in[0].gl_Position.xyz, gl_in[2].gl_Position.xyz,
                isFront234 ? gl_in[3].gl_Position.xyz : gl_in[4].gl_Position.xyz
        );
    }
    if (! isFront234) 
    {
        paintEdge(
                isFront012 ? gl_in[1].gl_Position.xyz : gl_in[0].gl_Position.xyz,
                gl_in[2].gl_Position.xyz, gl_in[4].gl_Position.xyz,
                isFront450 ? gl_in[5].gl_Position.xyz : gl_in[0].gl_Position.xyz
        );
    }
    if (! isFront450) 
    {
        paintEdge(
                isFront234 ? gl_in[3].gl_Position.xyz : gl_in[2].gl_Position.xyz,
                gl_in[4].gl_Position.xyz, gl_in[0].gl_Position.xyz,
                isFront012 ? gl_in[1].gl_Position.xyz : gl_in[2].gl_Position.xyz
        );
    }
}
