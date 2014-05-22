#version 120 // max supported on Snow Leopard

// pass-through vertex shader intended to recapitulate the 
// fixed-function OpenGL pipeline

varying vec4 outlineColor;

void main(void)
{
    outlineColor = gl_Color;
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
