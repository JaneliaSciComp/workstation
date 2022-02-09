// vertex shader to draw massive numbers of triangles.
#version 120

attribute vec4 vertexAttribute;
attribute vec4 normalAttribute;
attribute vec4 colorAttribute;

uniform mat4 projection;
uniform mat4 modelView;
uniform mat4 normalMatrix;
uniform vec4 color;
uniform int colorStrategy = 0;
varying vec4 colorVar;

varying vec4 normVar;
varying vec4 diffuseLightMag;

void main(void)
{
    normVar = normalize( normalMatrix * normalAttribute );
    if (colorStrategy == 1) 
    {
        colorVar = colorAttribute;
    }
    else
    {
        colorVar = color;
    }

    vec4 lightSource = normalize( vec4( 0, 0.0, 100.0, 0 ) - normVar );  // Behind "zNear".
    float diffuseCoefficient = max( 0.0, dot( normVar,lightSource ) );
    diffuseLightMag = vec4(1.0, 1.0, 1.0, 1.0) * 1.3 * diffuseCoefficient;
    gl_Position = projection * modelView * vertexAttribute;
}
