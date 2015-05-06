#version 330

layout (location=0) in vec4 iv;

uniform mat4 proj;
uniform mat4 view;
uniform mat4 model;

out vec4 position2;

void main()
{
    float xyoffset = -0.25;
    float zoffset = 0.25;
    position2 = vec4( ((iv.x+1.0)/2.0), ((iv.y+1.0)/2.0), ((iv.z+1.0)/2.0), 1.0);
    vec4 centeredIv = vec4( ((iv.x+1.0)/4.0)+xyoffset, ((iv.y+1.0)/4.0)+xyoffset, -1.0*(((iv.z+1.0)/4.0)+zoffset), 1.0);
    gl_Position = proj * view * model * centeredIv;
}