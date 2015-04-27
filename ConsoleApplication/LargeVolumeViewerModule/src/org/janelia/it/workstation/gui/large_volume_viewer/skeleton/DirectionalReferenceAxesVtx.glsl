// vertex shader to draw massive numbers of triangles.
#version 120

attribute vec4 vertexAttribute;
attribute vec4 colorAttribute;

uniform mat4 projection;
uniform mat4 modelView;

varying vec4 homogeniousCoordPos;
varying vec4 fragmentColor;

void main(void)
{
    homogeniousCoordPos = projection * modelView * vertexAttribute;
    fragmentColor = colorAttribute;
    gl_Position = homogeniousCoordPos;
}
