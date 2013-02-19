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
            // This maps the masking data value to a color set.
            float iIx = floor(maskingColor.g * 65535.1);
            float visY = floor(iIx / 256.0);
            float visX = (iIx - 256.0 * visY) / 256.0;
            vec3 cmCoord = vec3( visX, visY, 0.0 );
            vec4 mappedColor = texture3D(colorMapTexture, cmCoord);

            // This finds the render-method byte, which is stored in the alpha byte of the uploaded mapping texture.
            float renderMethod = floor(mappedColor[ 3 ] * 255.1);

            // Find the max intensity.
            vec4 signalColor = origColor;
            float maxIntensity = 0.0;
            for (int i = 0; i < 3; i++) {
                if ( signalColor[i] > maxIntensity )
                    maxIntensity = signalColor[i];
            }

            if ( mappedColor[ 3 ] == 0.0 ) {
                // Zero in the render method position.  This constitutes
                // an "off" switch.
                rtnVal[ 0 ] = 0.0;
                rtnVal[ 1 ] = 0.0;
                rtnVal[ 2 ] = 0.0;
            }
            else if ( renderMethod == 4.0 ) {
                rtnVal = origColor;
            }
            else if ( renderMethod == 3.0 ) {
                // Special case: solid compartment.
                rtnVal[ 0 ] = 0.3;
                rtnVal[ 1 ] = 0.3;
                rtnVal[ 2 ] = 0.3;
            }
            else if ( renderMethod == 2.0 ) {
                // Special case: a translucent compartment.  Here, make a translucent gray appearance.
                // For gray mappings, fill in solid gray for anything empty, but otherwise just use original.
                if ( maxIntensity < 0.05 ) {
                    mappedColor = vec4( 1.0, 1.0, 1.0, 1.0 );
                }
                else {
                    mappedColor = origColor; // TEMP?
                }

                for (int i = 0; i < 3; i++) {
                    rtnVal[i] = mappedColor[ i ] * maxIntensity;
                }
            }
            else if ( renderMethod == 1.0 ) {
                // Guard against empty signal texture.
                if ( maxIntensity == 0.0 ) {
                    maxIntensity = 1.0;
                }
                // This takes the mapped color, and multiplies it by the
                // maximum intensity of any signal color.
                for (int i = 0; i < 3; i++) {
                    rtnVal[i] = mappedColor[ i ] * maxIntensity;
                }
            }
            else {
                // Debug coloring.
                if ( renderMethod > 10.0 ) {
                    rtnVal[ 0 ] = 1.0;
                }
                else if ( renderMethod > 7.5 ) {
                    rtnVal[ 1 ] = 1.0;
                }
                else if ( renderMethod > 5.0 ) {
                    rtnVal[ 2 ] = 1.0;
                }
                else {
                    rtnVal[ 0 ] = 1.0;
                    rtnVal[ 1 ] = 1.0;
                    rtnVal[ 2 ] = 1.0;
                }
            }

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

