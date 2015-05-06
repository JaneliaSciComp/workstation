// Fragment shader for drawing mesh/triangles.
#version 120
varying vec4 homogeniousCoordPos;
varying vec4 fragmentColor;

void main()
{
    gl_FragColor = fragmentColor;
}
