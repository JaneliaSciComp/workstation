// vertex shader to draw massive numbers of triangles.
#version 120

attribute vec4 vertexAttribute;
attribute vec4 normalAttribute;

uniform mat4 projection;
uniform mat4 modelView;
uniform mat4 normalMatrix;

varying vec4 normVar;
varying vec4 diffuseLightMag;

void main(void)
{
    normVar = normalize( normalMatrix * normalAttribute );

    vec4 lightSource = vec4( 0, 0.0, 1.0, 0 );  // Behind "zNear".
    float diffuseCoefficient = dot( normVar,lightSource );
    if ( diffuseCoefficient < 0.0 )
    {
        diffuseCoefficient = abs( diffuseCoefficient );
    }
    diffuseLightMag = vec4(1.0, 1.0, 1.0, 1.0) * 1.3 * diffuseCoefficient;
    gl_Position = projection * modelView * vertexAttribute;
}
