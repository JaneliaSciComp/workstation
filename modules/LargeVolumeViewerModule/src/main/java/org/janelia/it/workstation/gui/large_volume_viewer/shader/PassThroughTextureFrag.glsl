#version 120 // max supported on Snow Leopard

uniform sampler2D tileTexture;

void main()
{
    gl_FragColor = texture2D(tileTexture, gl_TexCoord[0].xy).rgba;
}
