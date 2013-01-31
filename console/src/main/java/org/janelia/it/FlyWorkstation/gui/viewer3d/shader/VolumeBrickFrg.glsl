// Fragment shader for all filtering applied by volume brick.
uniform sampler3D signalTexture;
uniform sampler3D maskingTexture;
uniform sampler3D colorMapTexture;

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

        // Display strategy: only show the signal, only if the mask is non-zero.
        if ( ( ( maskingColor[0] + maskingColor[1] + maskingColor[2] ) == 0.0 ) ) {
            rtnVal[0] = 0.0;
            rtnVal[1] = 0.0;
            rtnVal[2] = 0.0;
        }
        else {
            // This reverts the value of this voxel to its original integer range.
//            int maskValue = int(floor(maskingColor.g*65535.0));

            // This maps the masking data value to a color set.
            float iIx = floor(maskingColor.g * 65535.1);
            float visY = floor(iIx / 256.0);
            float visX = (iIx - 256.0 * visY) / 256.0;
            vec3 cmCoord = vec3( visX, visY, 0.0 );
            vec4 mappedColor = texture3D(colorMapTexture, cmCoord);
            rtnVal = mappedColor;
//            rtnVal = maskingColor;
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

