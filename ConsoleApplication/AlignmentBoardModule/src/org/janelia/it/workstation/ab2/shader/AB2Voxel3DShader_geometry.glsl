#version 450

layout(points) in;
layout(triangle_strip, max_vertices=24) out;

in vec4 colorv[];
out vec4 colorg;

uniform mat4 mvp;
uniform vec3 voxelSize;

void main()
{
    vec4 base = gl_in[0].gl_Position;

    // FULL-CUBE QUAD-SET

    vec4 v000 = base;
    vec4 v010 = vec4(base.x,               base.y + voxelSize.y, base.z,               base.w);
    vec4 v100 = vec4(base.x + voxelSize.x, base.y,               base.z,               base.w);
    vec4 v110 = vec4(base.x + voxelSize.x, base.y + voxelSize.y, base.z,               base.w);

    vec4 v001 = vec4(base.x,               base.y,               base.z + voxelSize.z, base.w);
    vec4 v011 = vec4(base.x,               base.y + voxelSize.y, base.z + voxelSize.z, base.w);
    vec4 v101 = vec4(base.x + voxelSize.x, base.y,               base.z + voxelSize.z, base.w);
    vec4 v111 = vec4(base.x + voxelSize.x, base.y + voxelSize.y, base.z + voxelSize.z, base.w);

    vec4 v000p = mvp * v000;

//if (! (v000p.x > -0.5 && v000p.x < -0.4) ) {
    if (1.0>0.0) {

    vec4 v010p = mvp * v010;
    vec4 v100p = mvp * v100;
    vec4 v110p = mvp * v110;

    vec4 v001p = mvp * v001;
    vec4 v011p = mvp * v011;
    vec4 v101p = mvp * v101;
    vec4 v111p = mvp * v111;

    colorg = colorv[0];

    // Back
    gl_Position = v000p; EmitVertex();
    gl_Position = v100p; EmitVertex();
    gl_Position = v010p; EmitVertex();
    gl_Position = v110p; EmitVertex();
    EndPrimitive();

    // Front
    gl_Position = v001p; EmitVertex();
    gl_Position = v101p; EmitVertex();
    gl_Position = v011p; EmitVertex();
    gl_Position = v111p; EmitVertex();
    EndPrimitive();

    // Top
    gl_Position = v010p; EmitVertex();
    gl_Position = v110p; EmitVertex();
    gl_Position = v011p; EmitVertex();
    gl_Position = v111p; EmitVertex();
    EndPrimitive();

    // Bottom
    gl_Position = v000p; EmitVertex();
    gl_Position = v100p; EmitVertex();
    gl_Position = v001p; EmitVertex();
    gl_Position = v101p; EmitVertex();
    EndPrimitive();

    // Left
    gl_Position = v000p; EmitVertex();
    gl_Position = v001p; EmitVertex();
    gl_Position = v010p; EmitVertex();
    gl_Position = v011p; EmitVertex();
    EndPrimitive();

    // Right
    gl_Position = v100p; EmitVertex();
    gl_Position = v101p; EmitVertex();
    gl_Position = v110p; EmitVertex();
    gl_Position = v111p; EmitVertex();
    EndPrimitive();

    }

}
