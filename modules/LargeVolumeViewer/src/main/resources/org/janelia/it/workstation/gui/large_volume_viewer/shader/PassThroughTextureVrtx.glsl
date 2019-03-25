#version 120 // max supported on Snow Leopard

// pass-through vertex shader intended to recapitulate the 
// fixed-function OpenGL pipeline

void main(void)
{
    // gl_FrontColor = gl_Color;
    gl_TexCoord[0] = gl_MultiTexCoord0;
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
