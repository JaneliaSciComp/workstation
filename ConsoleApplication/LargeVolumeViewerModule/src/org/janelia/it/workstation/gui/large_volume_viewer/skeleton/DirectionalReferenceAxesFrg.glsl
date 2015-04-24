// Fragment shader for drawing mesh/triangles.
#version 120
uniform vec4 color = vec4(0.5,0.5,0.5, 1.0);
varying vec4 homogeniousCoordPos;

void main()
{
    gl_FragColor = color;
}
