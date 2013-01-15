// Fragment shader intended to allow dynamic selection of presented colors.
uniform sampler3D signalTexture;
uniform sampler3D maskingTexture;
uniform vec4 colorMask;
uniform int hasMaskingTexture;

// This takes the color as represented in the texture volume for this fragment and
// eliminates colors opted out by the user.
vec4 colorFilter()
{
    vec4 origColor = texture3D(signalTexture, gl_TexCoord[0].xyz);

    // Here, apply the color mask.
    origColor[0] = colorMask[0] * origColor[0];
    origColor[1] = colorMask[1] * origColor[1];
    origColor[2] = colorMask[2] * origColor[2];
    //gl_FragColor = origColor;
    return origColor;
}

// This takes the results of the color filter and creates "coloring relief" of areas given by the
// masking texture.
vec4 volumeMask(vec4 origColor)
{
    if (hasMaskingTexture > 0)
    {
        vec4 maskingColor = texture3D(maskingTexture, gl_TexCoord[0].xyz);
        if (maskingColor[0] * maskingColor[1] * maskingColor[2] == 0.0)
        {
            origColor[3] *= 0.5;
        }
    }
    return origColor;
}

void main()
{
// NO-OP
    gl_FragColor = gl_TexCoord[0].xyz;

//    origColor = colorFilter();
//    gl_FragColor = volumeMask(origColor);

}

