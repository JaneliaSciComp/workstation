#version 430

layout(points) in;
layout(triangle_strip, max_vertices=3) out;

uniform mat4 mvp;
uniform vec3 voxelUnitSize;

void main()
{
    vec4 v0 = gl_in[0].gl_Position;
    vec4 v1 = vec4(v0.x, v0.y + voxelUnitSize.y, v0.z, v0.w);
    vec4 v2 = vec4(v0.x + voxelUnitSize.x, v0.y, v0.z, v0.w);

    gl_Position = mvp * v0;
    EmitVertex();

    gl_Position = mvp * v1;
    EmitVertex();

    gl_Position = mvp * v2;
    EmitVertex();

    EndPrimitive();
}