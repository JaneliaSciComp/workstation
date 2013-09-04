// Fragment shader for all filtering applied by volume brick.
#version 120
uniform sampler3D signalTexture;
uniform sampler3D maskingTexture;
uniform sampler3D colorMapTexture;

uniform float gammaAdjustment = 1.0;
uniform float cropOutLevel = 0.05;
uniform int hasMaskingTexture;

// "Cropping" region driven by feeds into the shader.
// As soon as any cropping is done, ALL the below must have values in other than -1.
uniform float startCropX = -1.0;
uniform float endCropX = 1.0;
uniform float startCropY = -1.0;
uniform float endCropY = 1.0;
uniform float startCropZ = -1.0;
uniform float endCropZ = 1.0;

vec3 getMapCoord( float location )
{
    float visY = floor(location / 256.0);
    float visX = (location - 256.0 * visY) / 256.0;
    visY = visY / 258.1; // 259.0
    vec3 cmCoord = vec3( visX, visY, 0.0 );
    return cmCoord;
}

float computeMaxIntensity( vec4 inputColor ) {
    float maxIntensity = 0.0;
    for (int i = 0; i < 3; i++)
    {
        if ( inputColor[i] > maxIntensity )
            maxIntensity = inputColor[i];
    }
    return maxIntensity;
}

// This takes the results of the color filter and creates "coloring relief" of areas given by the
// masking texture.
vec4 volumeMask(vec4 origColor)
{
    vec4 rtnVal = origColor;
    if (hasMaskingTexture > 0)
    {
        // texture3D returns vec4.
        vec4 maskingColor = texture3D(maskingTexture, gl_TexCoord[0].xyz);

        if ( ( ( origColor[0] + origColor[1] + origColor[2] ) == 0.0 ) )
        {
            // Display strategy: bypass unseen values.
            discard;
        }
        else
        {
            // Enter here -> original color is not black.

            // This maps the masking data value to a color set.
            vec3 cmCoord = getMapCoord( floor( maskingColor.g * 65535.1 ) );
            vec4 mappedColor = texture3D(colorMapTexture, cmCoord);

            // This finds the render-method byte, which is stored in the alpha byte of the uploaded mapping texture.
            float renderMethod = floor(mappedColor[ 3 ] * 255.1);

            // Find the max intensity.
            vec4 signalColor = origColor;
            float maxIntensity = computeMaxIntensity( signalColor );

            if ( mappedColor[ 3 ] == 0.0 )
            {
                // This constitutes an "off" switch.
                discard;
            }
            else if ( renderMethod == 4.0 )
            {
                float multiplier = 0.4 * maxIntensity;
                rtnVal = vec4( origColor[ 0 ] * multiplier, origColor[ 1 ] * multiplier, origColor[ 2 ] * multiplier, maxIntensity );
            }
            else if ( renderMethod == 3.0 )
            {
                // Special case: solid compartment.
                rtnVal[ 0 ] = 0.3;
                rtnVal[ 1 ] = 0.3;
                rtnVal[ 2 ] = 0.3;
            }
            else if ( renderMethod == 2.0 )
            {
                float maxMappedIntensity = computeMaxIntensity( mappedColor );

                // Special case: a translucent compartment.  Here, make a translucent gray appearance.
                // For gray mappings, fill in solid gray for anything empty, but otherwise just use original.
                if ( maxMappedIntensity < 0.05 )
                {
                    mappedColor = vec4( 3.0, 3.0, 3.0, 1.0 ); // Need to "overpower" the gamma correction with >1 val
                }

                for (int i = 0; i < 3; i++)
                {
                    rtnVal[i] = mappedColor[ i ] * 0.1 * maxIntensity;
                }
            }
            else if ( renderMethod == 1.0 )
            {
                // This takes the mapped color, and multiplies it by the
                // maximum intensity of any signal color.
                for (int i = 0; i < 3; i++)
                {
                    rtnVal[i] = mappedColor[ i ] * maxIntensity;
                }
            }
            else
            {
                // Debug coloring.
                rtnVal[ 0 ] = 1.0;
                rtnVal[ 1 ] = 1.0;
                rtnVal[ 2 ] = 1.0;
            }

        }
    }
    else if ( ( origColor[0] + origColor[1] + origColor[2] ) == 0.0 )
    {
        discard;
    }

    return rtnVal;
}

vec4 gammaAdjust(vec4 origColor)
{
    vec4 adjustedColor = vec4(
        pow(origColor[0], gammaAdjustment),
        pow(origColor[1], gammaAdjustment),
        pow(origColor[2], gammaAdjustment),
        origColor[3]
    );

    return adjustedColor;
}

float getAxialCoord(int coordSetLoc)
{
    vec3 cmCoord = getMapCoord( coordSetLoc );
    vec4 axialCoordVec = texture3D( colorMapTexture, cmCoord );

    // To get each coord across to the shader in a normalized float, its normalized version
    // was first multiplied by a standard value.   Note that there was an automatic division by
    // 255 in each float making up the axialCoordVec "color".  Therefore we do not divide overall
    // by the number used to multiply on the CPU side, but rather by _that number_ divided by 256.
    float axialCoord = (axialCoordVec[0]
                       + (axialCoordVec[1] * 255.1)
                       + (axialCoordVec[2] * 65535.1)
                       + (axialCoordVec[3] * 16777215.1) )
          / 8.0;   // Now divide by this standard value  2048 / 256 = 8
    return axialCoord;
}

bool getInCrop(vec3 point, float pStartCropX, float pEndCropX, float pStartCropY, float pEndCropY, float pStartCropZ, float pEndCropZ)
{
    bool inCrop = true;
    if ( pStartCropX > -0.5 )
    {
        if ( point.x < pStartCropX )
        {
            inCrop = false;
        }
        else if ( point.x > pEndCropX )
        {
            inCrop = false;
        }
        else if ( point.y < pStartCropY )
        {
            inCrop = false;
        }
        else if ( point.y > pEndCropY )
        {
            inCrop = false;
        }
        else if ( point.z < pStartCropZ )
        {
            inCrop = false;
        }
        else if ( point.z > pEndCropZ )
        {
            inCrop = false;
        }
    }
    return inCrop;
}

vec4 crop(vec4 origColor)
{
    vec3 point = gl_TexCoord[ 0 ].xyz;
    bool inCrop = getInCrop(point, startCropX, endCropX, startCropY, endCropY, startCropZ, endCropZ);

    if ( ! inCrop )
    {
        // Not in the current user selection.  Try all saved selections.
        int nextCoordSetLoc = 65535 + 1;  // Weird OS X Bug does not like 65536

        // Up to max possible crops, or bail-on-signal.
        for ( int i = 0; (! inCrop) && i < 192; i++ )
        {

            float axialStX = getAxialCoord(nextCoordSetLoc);

            float axialEnX = getAxialCoord(nextCoordSetLoc + 1);
            float axialStY = getAxialCoord(nextCoordSetLoc + 2);
            float axialEnY = getAxialCoord(nextCoordSetLoc + 3);
            float axialStZ = getAxialCoord(nextCoordSetLoc + 4);
            float axialEnZ = getAxialCoord(nextCoordSetLoc + 5);

            if (axialStX<=0.0 && axialEnX<=0.0 && axialStY<=0.0 && axialEnY<=0.0 && axialStZ<=0.0 && axialEnZ<=0.0)
            {
                break; // All-zero is NO crop-box.
            }
            inCrop = getInCrop(point, axialStX, axialEnX, axialStY, axialEnY, axialStZ, axialEnZ);

            nextCoordSetLoc += 6;
        }
    }

    if ( inCrop == true )
    {
        return origColor;
    }
    else
    {
        // Very light crop color.
        return vec4( cropOutLevel * origColor.x, cropOutLevel * origColor.y, cropOutLevel * origColor.z, 1.0 );
    }
}

void main()
{
    vec4 origColor = texture3D(signalTexture, gl_TexCoord[0].xyz);
    vec4 maskedColor = volumeMask(origColor);
    vec4 gammaAdjustedColor = gammaAdjust(maskedColor);
    gl_FragColor = crop(gammaAdjustedColor);

}

