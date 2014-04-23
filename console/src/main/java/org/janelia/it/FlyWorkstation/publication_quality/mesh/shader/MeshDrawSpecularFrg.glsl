// Fragment shader for drawing mesh/triangles.
#version 120
uniform vec4 color;
varying vec4 diffuseLightMag;
varying vec4 specularLightMag;

void main()
{
    vec4 finalColor = (color * diffuseLightMag);
    if ( length(specularLightMag) > 0 )
    {
        finalColor += specularLightMag;
    }
    gl_FragColor = finalColor;
    gl_FragColor.w = 1.0; // Force alpha to 1.
}
