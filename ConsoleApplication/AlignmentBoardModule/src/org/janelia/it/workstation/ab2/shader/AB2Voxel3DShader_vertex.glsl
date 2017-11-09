#version 450

layout (location=0) in vec3 fv;
layout (location=1) in vec4 fc;

uniform vec3 dimXYZ;
uniform vec3 voxelSize;

out vec4 colorv;

void main()
{
    colorv=fc;
    gl_Position = vec4(fv.x*voxelSize.x, fv.y*voxelSize.y, fv.z*voxelSize.z, 1.0);
}
