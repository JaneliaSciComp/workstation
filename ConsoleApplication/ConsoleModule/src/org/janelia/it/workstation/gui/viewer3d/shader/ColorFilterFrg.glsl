// Fragment shader intended to allow dynamic selection of presented colors.
uniform sampler3D volumeTexture;
uniform vec4 colorMask;

vec4 srgb( vec4 origColor )
{
    vec4 rtnVal = vec4(pow(origColor.r, 0.46), pow(origColor.g, 0.46), pow(origColor.b, 0.46), 1.0);
    return rtnVal;
}

void main()
{
    vec4 origColor = texture3D(volumeTexture, gl_TexCoord[0].xyz);
    // "Mask" is, at time-of-writing, really a flag to completely turn the color on/off.  However,
    // were anyone to apply an intermediate value, like 0.5, the intensities of the colors could in fact be
    // affected here.
    origColor[0] = colorMask[0] * origColor[0];
    origColor[1] = colorMask[1] * origColor[1];
    origColor[2] = colorMask[2] * origColor[2];
    gl_FragColor = srgb( origColor );
}
