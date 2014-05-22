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

float computeMaxIntensity( vec4 inputColor )
{
    float maxIntensity = 0.0;
    for (int i = 0; i < 3; i++)
    {
        if ( inputColor[i] > maxIntensity )
            maxIntensity = inputColor[i];
    }
    return maxIntensity;
}


float posInterpToBitCount( float posInterp )
{ // Map the interpretation of position into a bit-count.
    if ( posInterp == 0.0 )
    {
        return 24.0;
    }
    else if ( posInterp == 1.0 )
    {
        return 8.0;
    }
    else if ( posInterp == 2.0 )
    {
        return 4.0;
    }
}

float lowBitAdd(float origVal, float testByte, float addOnSet)
{
    float rtnVal = origVal;
    float bitVal = mod( testByte, 2.0 );
    if ( bitVal == 1.0 )
    {
        rtnVal = rtnVal + addOnSet;
    }

    return rtnVal;
}

float computeIntensity( vec4 inputColor, float pos, float posInterp )
{   // Get the intensity bits.
    float rtnVal = 0.0;
    if ( posInterp == 0.0 )
    {
        rtnVal = computeMaxIntensity( inputColor );
    }
    else
    {
        float bitCount = posInterpToBitCount( posInterp );
        if ( bitCount == 8.0 )
        {
            rtnVal = inputColor[ int(pos) ];
        }
        else if ( bitCount == 4.0 )
        {   // Must do a calc down into the 4-bit locs.
            float byteInx = pos / 2.0;
            float byteInxFloor = floor( pos / 2.0 );
            float byteUsed = inputColor[ int( byteInxFloor ) ];
            byteUsed *= 256.0;
            if ( byteInxFloor == byteInx )
            {   // Bottom nibble.
//                float topNibble = floor( byteUsed / 16.0 );
//                rtnVal = byteUsed - (topNibble * 16);
                for (int i = 0; i < 4; i++)
                {
                    rtnVal = lowBitAdd( rtnVal, byteUsed, pow( 2.0, float( i ) ) );
                    // Must now pop the bottom bit off the "byte used" byte.
                    byteUsed = floor( byteUsed / 2.0 );
                }
            }
            else
            {   // Top nibble.
                rtnVal = floor( byteUsed / 16.0 );
            }

            // Now, must re-expand the value found above, to cover the 8-bit range.
            rtnVal = rtnVal * 16.0 / 256.0; // Re-normalize the output.
        }
    }

    return rtnVal;
}

// This takes the results any previous steps and creates "coloring relief" of areas given by the
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

            // Three masked-in values below will be "popped off" as if from a stack of bits.

            // This finds the render-method byte, which is stored in the high byte of the uploaded mapping texture.
            float renderMethodByte = floor(mappedColor[ 3 ] * 255.1);

            float renderMethod = 0.0;
            renderMethod = lowBitAdd( renderMethod, renderMethodByte, 1.0 );

            renderMethodByte = floor( renderMethodByte / 2.0 );
            renderMethod = lowBitAdd( renderMethod, renderMethodByte, 2.0 );

            renderMethodByte = floor( renderMethodByte / 2.0 );
            renderMethod = lowBitAdd( renderMethod, renderMethodByte, 4.0 );

            // Gather the intensity position.
            float intensityPos = 0.0;
            renderMethodByte = floor( renderMethodByte / 2.0 );
            intensityPos = lowBitAdd( intensityPos, renderMethodByte, 1.0 );

            renderMethodByte = floor( renderMethodByte / 2.0 );
            intensityPos = lowBitAdd( intensityPos, renderMethodByte, 2.0 );

            renderMethodByte = floor( renderMethodByte / 2.0 );
            intensityPos = lowBitAdd( intensityPos, renderMethodByte, 4.0 );

            // Gather the intensity position interpretation.
            float posInterp = 0.0;
            renderMethodByte = floor( renderMethodByte / 2.0 );
            posInterp = lowBitAdd( posInterp, renderMethodByte, 1.0 );

            renderMethodByte = floor( renderMethodByte / 2.0 );
            posInterp = lowBitAdd( posInterp, renderMethodByte, 2.0 );

            // Find the max intensity.
            vec4 signalColor = origColor;
            float intensity = computeIntensity( signalColor, intensityPos, posInterp );

            if ( mappedColor[ 3 ] == 0.0 )
            {
                // This constitutes an "off" switch.
                discard;
            }
            else if ( renderMethod == 4.0 )
            {
                float multiplier = 0.4 * intensity;
                rtnVal = vec4( origColor[ 0 ] * multiplier, origColor[ 1 ] * multiplier, origColor[ 2 ] * multiplier, intensity );
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
                float mappedIntensity = computeMaxIntensity( mappedColor );

                // Special case: a translucent compartment.  Here, make a translucent gray appearance.
                // For gray mappings, fill in solid gray for anything empty, but otherwise just use original.
                for (int i = 0; i < 3; i++)
                {
                    rtnVal[i] = mappedColor[ i ] * 0.1 * intensity;
                }
            }
            else if ( renderMethod == 1.0 )
            {
                // This takes the mapped color, and multiplies it by the
                // maximum intensity of any signal color.
                for (int i = 0; i < 3; i++)
                {
                    rtnVal[i] = mappedColor[ i ] * intensity;
                }
                rtnVal[3] = intensity;
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

