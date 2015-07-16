// vertex shader to draw massive numbers of triangles.
#version 120

attribute vec4 vertexAttribute;
attribute vec4 normalAttribute;
attribute vec4 colorAttribute;
attribute float idAttribute;

uniform mat4 projection;
uniform mat4 modelView;
uniform mat4 normalMatrix;
uniform vec4 color;
uniform int colorStrategy;
uniform int idsAvailable;

varying vec4 normVar;
varying vec4 colorVar;
varying vec4 diffuseLightMag;
varying vec4 specularLightMag;
varying vec4 homogeniousCoordPos;
varying float id;

void main(void)
{
    homogeniousCoordPos = projection * modelView * vertexAttribute;
    normVar = normalize( normalMatrix * normalAttribute );
    if (colorStrategy == 1) 
    {
        colorVar = colorAttribute;
    }
    else
    {
        colorVar = color;
    }

    if (idsAvailable == 1)
    {
        id = idAttribute;
    }
    else
    {
        id = 0;
    }

    vec4 lightPosition = vec4( 0, 0.0, 100.0, 0 );
    vec4 lightSource = normalize( lightPosition );  // Behind "zNear".

    // From diffuse lighting.
    float diffuseCoefficient = max( 0.0, dot( normVar,lightSource ) );
    diffuseLightMag = vec4(1.0, 1.0, 1.0, 1.0) * 1.3 * diffuseCoefficient;
    gl_Position = homogeniousCoordPos;
}
