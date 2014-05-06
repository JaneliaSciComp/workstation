// Fragment shader for drawing mesh/triangles.
#version 120
uniform vec4 color;
varying vec4 diffuseLightMag;
varying vec4 homogeniousCoordPos;
varying vec4 normVar;

void main()
{
    vec4 finalColor = (color * diffuseLightMag);

    // Calculate the specular component.
    // Using unnormalized version of eye-normal.
    vec4 specularLightMag;
    if ( length(diffuseLightMag) > 0.0 )
    {
        vec4 toLight = vec4( 0, 0.0, 100.0, 0 ) - homogeniousCoordPos;
        vec4 toV = -normalize(vec4(homogeniousCoordPos.xyz, 0));
        toLight = normalize(toLight);
        vec4 halfVector = normalize(toV + toLight);

        specularLightMag = vec4(1.0,1.0,1.0,0.0) * pow(max(0.0, -dot(halfVector, normVar)), 64.0);
    }

    if ( length(specularLightMag) > 0 )
    {
        finalColor += specularLightMag;
    }
    gl_FragColor = finalColor;
    gl_FragColor.w = 1.0; // Force alpha to 1.
}
