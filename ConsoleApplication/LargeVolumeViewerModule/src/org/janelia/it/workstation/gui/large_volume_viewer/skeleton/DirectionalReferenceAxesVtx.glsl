// vertex shader to draw massive numbers of triangles.
#version 120

attribute vec4 vertexAttribute;

uniform mat4 projection;
uniform mat4 modelView;

varying vec4 homogeniousCoordPos;

void main(void)
{
    homogeniousCoordPos = projection * modelView * vertexAttribute;
    gl_Position = homogeniousCoordPos;
}
