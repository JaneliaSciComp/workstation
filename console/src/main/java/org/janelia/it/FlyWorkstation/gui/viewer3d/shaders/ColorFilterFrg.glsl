// Fragment shader intended to allow dynamic selection of presented colors.
uniform sampler3D volumeTexture;

void main()
{
//    gl_FragColor = texture3D(volumeTexture, gl_TexCoord[0].xyz);
    gl_FragColor = vec4( 0.0, 1.0, 0.0, 1.0 );
}
