// vertex shader to support dynamic selection of presented colors.
#version 120
uniform int hasMaskingTexture;

void main(void)
{
    gl_FrontColor = gl_Color;
    gl_TexCoord[0] = gl_MultiTexCoord0;
    if (hasMaskingTexture > 0) {
        gl_TexCoord[1] = gl_MultiTexCoord1;
    }
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
