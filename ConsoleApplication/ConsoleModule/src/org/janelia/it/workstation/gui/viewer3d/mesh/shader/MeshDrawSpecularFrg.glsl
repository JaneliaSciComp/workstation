// Fragment shader for drawing mesh/triangles.
#version 120
varying vec4 colorVar;
varying vec4 diffuseLightMag;
varying vec4 homogeniousCoordPos;
varying vec4 normVar;
varying float id;

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
    gl_FragColor = finalColor;
    if ( id > 0 )
    {
        gl_FragColor.r = id; // using red as pure-ID color
        gl_FragColor.g = id;
        gl_FragColor.b = id;
    }
    gl_FragColor.w = 1.0; // Force alpha to 1.
}
