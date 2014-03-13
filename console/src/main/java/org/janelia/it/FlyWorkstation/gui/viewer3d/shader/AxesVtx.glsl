// vertex shader to support dynamic selection of presented colors.
#version 120

attribute vec4 vertexAttribute;

uniform mat4 projection;
uniform mat4 modelView;

void main(void)
{
    gl_Position = projection * modelView * vertexAttribute;
}
