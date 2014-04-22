// Fragment shader for drawing mesh/triangles.
#version 120
uniform vec4 color;
varying vec4 diffuseLightMag;

void main()
{
    gl_FragColor = (color * diffuseLightMag);
    gl_FragColor.w = 1.0; // Force alpha to 1.
}
