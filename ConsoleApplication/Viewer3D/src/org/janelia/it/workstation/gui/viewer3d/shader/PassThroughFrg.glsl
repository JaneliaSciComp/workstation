
uniform sampler3D volumeTexture;

void main()
{
    gl_FragColor = texture3D(volumeTexture, gl_TexCoord[0].xyz);
}
