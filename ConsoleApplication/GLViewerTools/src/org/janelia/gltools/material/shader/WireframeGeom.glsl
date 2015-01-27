#version 330

// Advanced wireframe geometry shader
// TODO - convert triangle edges to quads

uniform mat4 projectionMatrix = mat4(1);
uniform float lineWidth = 0.1;

layout(triangles) in;
layout(triangle_strip, max_vertices=12) out;

void paintEdge(vec3 a, vec3 b) {
    vec3 x = 0.5 * lineWidth * normalize(b - a); // along edge
    vec3 y = 0.5 * lineWidth * normalize(cross(x, 0.5 * (a+b))); // perpendicular to view
    gl_Position = projectionMatrix * vec4(b + x - y, 1);
    EmitVertex();
    gl_Position = projectionMatrix * vec4(b + x + y, 1);
    EmitVertex();
    gl_Position = projectionMatrix * vec4(a - x - y, 1);
    EmitVertex();
    gl_Position = projectionMatrix * vec4(a - x + y, 1);
    EmitVertex();
    EndPrimitive();
}

void main() 
{
    paintEdge(gl_in[0].gl_Position.xyz, gl_in[1].gl_Position.xyz);
    paintEdge(gl_in[1].gl_Position.xyz, gl_in[2].gl_Position.xyz);
    paintEdge(gl_in[2].gl_Position.xyz, gl_in[0].gl_Position.xyz);
}
