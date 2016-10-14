// Fragment shader for drawing mesh/triangles.
#version 120
uniform int idsAvailable;
varying vec4 colorVar;
varying vec4 diffuseLightMag;
varying vec4 homogeniousCoordPos;
varying vec4 normVar;
varying vec4 id;

void main()
{
    vec4 finalColor = (colorVar * diffuseLightMag);

    // Calculate the specular component.
    // Using unnormalized version of eye-normal.
    vec4 specularLightMag;
    if ( length(diffuseLightMag) > 0.0 )
    {
        vec4 toLight = vec4( 0, 0.0, 100.0, 0 ) - homogeniousCoordPos;
        vec4 toV = -normalize(vec4(homogeniousCoordPos.xyz, 0));
        toLight = normalize(toLight);
        vec4 halfVector = normalize(toV + toLight);

        specularLightMag = vec4(0.3,0.3,0.3,0.0) * pow(max(0.0, -dot(halfVector, normVar)), 4.0);
    }

    if ( length(specularLightMag) > 0 )
    {
        finalColor += specularLightMag;
    }

    // Drawing both to buffer 0 and buffer 1. Each buffer represents a different
    // "Render Target".  0=shaded color; 1=identifier
    gl_FragData[0] = finalColor;
    if ( idsAvailable == 1 )
    {
        gl_FragData[1] = id;
        gl_FragData[1].w = 1.0;
    }
    gl_FragData[0].w = 1.0;
}
