// vertex shader to support dynamic selection of presented colors.
#version 120

attribute vec4 vertexAttribute;
attribute vec4 texCoordAttribute;

uniform mat4 projection;
uniform mat4 modelView;

void main(void)
{
    gl_FrontColor = gl_Color;
    gl_TexCoord[0] = texCoordAttribute;
    gl_Position = projection * modelView * vertexAttribute;
}
