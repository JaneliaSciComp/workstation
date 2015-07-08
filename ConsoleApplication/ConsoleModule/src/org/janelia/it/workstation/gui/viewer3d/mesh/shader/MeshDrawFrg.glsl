// Fragment shader for drawing mesh/triangles.
#version 120
varying vec4 colorVar;
varying vec4 diffuseLightMag;

void main()
{
    gl_FragColor = (colorVar * diffuseLightMag);
    gl_FragColor.w = 1.0; // Force alpha to 1.
}
