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
varying vec4 homogeniousCoordPos;

void main(void)
{
    homogeniousCoordPos = projection * modelView * vertexAttribute;
    normVar = normalize( normalMatrix * normalAttribute );

    vec4 lightPosition = vec4( 0, 0.0, 100.0, 0 );
    vec4 lightSource = normalize( lightPosition );  // Behind "zNear".

    // From diffuse lighting.
    float diffuseCoefficient = max( 0.0, dot( normVar,lightSource ) );
    diffuseLightMag = vec4(1.0, 1.0, 1.0, 1.0) * 1.3 * diffuseCoefficient;
    gl_Position = homogeniousCoordPos;
}
