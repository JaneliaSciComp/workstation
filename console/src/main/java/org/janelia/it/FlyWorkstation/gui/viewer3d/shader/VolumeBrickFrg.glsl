// Fragment shader for all filtering applied by volume brick.
uniform sampler3D signalTexture;
uniform sampler3D maskingTexture;

uniform vec4 colorMask;
uniform int hasMaskingTexture;

// This takes the color as represented in the texture volume for this fragment and
// eliminates colors opted out by the user.
vec4 colorFilter()
{
    // texture3D returns vec4.
    vec4 origColor = texture3D(signalTexture, gl_TexCoord[0].xyz);

    // Here, apply the color mask.
    origColor[0] = colorMask[0] * origColor[0];
    origColor[1] = colorMask[1] * origColor[1];
    origColor[2] = colorMask[2] * origColor[2];
    return origColor;
}

// This takes the results of the color filter and creates "coloring relief" of areas given by the
// masking texture.
vec4 volumeMask(vec4 origColor)
{
    vec4 rtnVal = origColor;
    if (hasMaskingTexture > 0)
    {
        // texture3D returns vec4.
        vec4 maskingColor = texture3D(maskingTexture, gl_TexCoord[1].xyz);
        //maskingColor = texture3D(signalTexture, gl_TexCoord[1].xyz);

        // Display strategy: only show the signal, only if the mask is non-zero.
        if ( ( maskingColor[0] + maskingColor[1] + maskingColor[2] ) == 0.0 ) {
            rtnVal = origColor;
//            rtnVal[0] = 0.0;
//            rtnVal[1] = 0.0;
//            rtnVal[2] = 0.0;
        }
        else {
//            rtnVal[3] = maskingColor[ 0 ];
            rtnVal = maskingColor;
//            rtnVal[3] = maskingColor[3];
//            rtnVal[0] = maskingColor[3];
//            rtnVal[1] = 0.0;
//            rtnVal[2] = 0.0;
//            rtnVal[1] = maskingColor[1];
//            rtnVal[2] = maskingColor[2];
        }
    }

    return rtnVal;
}

void main()
{
    // NOTE: if the use of colorMask is commented away, the shader Java counterpart
    //  will fail to find the location of that uniform.  Hence, uncomment below if
    //  that inexplicably happens.
    vec4 throwAwayUse = colorMask;
// NO-OP
//    gl_FragColor = texture3D(signalTexture, gl_TexCoord[0].xyz);

    vec4 origColor = colorFilter();
//    gl_FragColor = origColor;
    gl_FragColor = volumeMask(origColor);

}

