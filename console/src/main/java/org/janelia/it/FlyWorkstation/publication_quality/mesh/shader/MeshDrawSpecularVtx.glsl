// vertex shader to draw massive numbers of triangles.
#version 120

attribute vec4 vertexAttribute;
attribute vec4 normalAttribute;

uniform mat4 projection;
uniform mat4 modelView;
uniform mat4 normalMatrix;

varying vec4 normVar;
varying vec4 diffuseLightMag;
varying vec4 specularLightMag;

void main(void)
{
    vec4 homogeniousCoordPos = projection * modelView * vertexAttribute;
    normVar = normalize( normalMatrix * normalAttribute );

    vec4 lightSource = normalize( vec4( 0, 0.0, 100.0, 0 ) - normVar );  // Behind "zNear".

    // From diffuse lighting.
    float diffuseCoefficient = max( 0.0, dot( normVar,lightSource ) );
    diffuseLightMag = vec4(1.0, 1.0, 1.0, 1.0) * 1.3 * diffuseCoefficient;

    // Calculate the specular component.
    // Using unnormalized version of eye-normal.
    vec4 reflection = normalize(reflect(-lightSource, normVar));
    if ( diffuseCoefficient > 0 )
    {
        float spec = max(0.0, dot(normVar,reflection));
        spec = pow(spec, 128.0);
        specularLightMag = vec4(spec,spec,spec,1.0);
    }
    gl_Position = homogeniousCoordPos;
}
