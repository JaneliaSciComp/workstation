#version 430

layout(points) in;

//layout(points, max_vertices=1) out;
layout(triangle_strip, max_vertices=4) out;
//layout(triangle_strip, max_vertices=24) out;

in float intensityG;
out float intensityF;
out float vz;

uniform mat4 mvp;
uniform mat4 proj;
uniform vec3 voxelUnitSize;

#define GTYPE 2

void main()
{
    vec4 base = gl_in[0].gl_Position;
    vec4 basePTest = vec4(base.x + voxelUnitSize.x, base.y + voxelUnitSize.y, base.z, base.w);

if (GTYPE==1) {

    vec4 test1 = proj * base;
    vec4 test2 = proj * basePTest;

    vec4 v00 = mvp * base;
    float xD = abs(test2.x - test1.x);
    float yD = abs(test2.y - test1.y);

    gl_PointSize = xD;
    vz = v00.z;
    gl_Position = v00;
    intensityF = intensityG;
    EmitVertex();
    EndPrimitive();
}

if (GTYPE==2) {

    // SINGLE-QUAD EXPERIMENT

    vec4 test1 = proj * base;
    vec4 test2 = proj * basePTest;

    vec4 v00 = mvp * base;
    float xD = abs(test2.x - test1.x);
    float yD = abs(test2.y - test1.y);

    vec4 v10 = vec4(v00.x + xD,       v00.y, v00.z, v00.w);
    vec4 v01 = vec4(v00.x     ,  v00.y + yD, v00.z, v00.w);
    vec4 v11 = vec4(v00.x + xD,  v00.y + yD, v00.z, v00.w);

    

    gl_Position = v00; vz = v00.z; intensityF = intensityG; EmitVertex();
    gl_Position = v10; vz = v10.z; intensityF = intensityG; EmitVertex();
    gl_Position = v01; vz = v01.z; intensityF = intensityG; EmitVertex();
    gl_Position = v11; vz = v11.z; intensityF = intensityG; EmitVertex();
    EndPrimitive();

}

if (GTYPE==3) {

    // FULL-CUBE QUAD-SET

    vec4 v000 = base;
    vec4 v010 = vec4(base.x, base.y + voxelUnitSize.y, base.z, base.w);
    vec4 v100 = vec4(base.x + voxelUnitSize.x, base.y, base.z, base.w);
    vec4 v110 = vec4(base.x + voxelUnitSize.x, base.y + voxelUnitSize.y, base.z, base.w);

    vec4 v001 = vec4(v000.x, v000.y, v000.z + voxelUnitSize.z, base.w);
    vec4 v011 = vec4(v010.x, v010.y, v010.z + voxelUnitSize.z, base.w);
    vec4 v101 = vec4(v100.x, v100.y, v100.z + voxelUnitSize.z, base.w);
    vec4 v111 = vec4(v110.x, v110.y, v110.z + voxelUnitSize.z, base.w);

    vec4 v000p = mvp * v000;
    vec4 v010p = mvp * v010;
    vec4 v100p = mvp * v100;
    vec4 v110p = mvp * v110;

    vec4 v001p = mvp * v001;
    vec4 v011p = mvp * v011;
    vec4 v101p = mvp * v101;
    vec4 v111p = mvp * v111;

    // Back
    gl_Position = v000p; EmitVertex();
    gl_Position = v010p; EmitVertex();
    gl_Position = v100p; EmitVertex();
    gl_Position = v110p; EmitVertex();
    EndPrimitive();

    // Front
    gl_Position = v001p; EmitVertex();
    gl_Position = v011p; EmitVertex();
    gl_Position = v101p; EmitVertex();
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