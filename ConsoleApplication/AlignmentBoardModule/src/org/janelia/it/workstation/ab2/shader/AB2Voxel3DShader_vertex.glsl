#version 450

layout (location=0) in ivec3 iv;
layout (location=1) in ivec4 ic;

uniform vec3 dimXYZ;

out vec4 colorv;

void main()
{
    vec4 fc=vec4(ic*1f);
    colorv=vec4(fc/255f);

    gl_Position = vec4( (iv.x*1f)/dimXYZ.x, (iv.y*1f)/dimXYZ.y, (iv.z*1f)/dimXYZ.z, 1.0);
}
