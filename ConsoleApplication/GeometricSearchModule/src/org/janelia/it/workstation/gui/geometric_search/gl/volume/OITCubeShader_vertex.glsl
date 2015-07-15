#version 430

uniform mat4 mvp; 

//uniform mat4 proj;
//uniform mat4 view;
//uniform mat4 model;

layout (location=0) in vec3 iv;

void main()
{
    vec4 vertexPosition = vec4( iv.x, iv.y, iv.z, 1.0);
    //gl_Position = proj * view * model * vertexPosition;
    gl_Position = mvp * vertexPosition;
}