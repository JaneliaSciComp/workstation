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
    vec4 posVector = vec4(homogeniousCoordPos.xyz, 0);

    normVar = normalize( normalMatrix * normalAttribute );

    vec4 diffuseLightSource = normalize( vec4( 0, 0.0, 1.0, 0 ) - posVector );  // Behind "zNear".
    //vec4 specularLightSource = vec4( 0.57777, 0.57777, 0.57777, 0 ); // From front upper right octant
    vec4 specularLightSource = vec4( 0, 0.0, 1.0, 0 ); // Behind "zNear"
    //vec4 specularLightSource = vec4( 0.7071, 0.7071, 0, 0 ); // From upper right quadrant (no z)

    // Calculate the diffuse component.
    float diffuseCoefficient = dot( normVar,diffuseLightSource );
    //if ( diffuseCoefficient < 0.0 )
    //{
    //    diffuseCoefficient = abs( diffuseCoefficient );
    //}
    diffuseLightMag = vec4(1.0, 1.0, 1.0, 1.0) * 1.3 * diffuseCoefficient;

    // Calculate the specular component.
    vec4 specularLightDirection = normalize(specularLightSource - posVector);
    // Using unnormalized version of eye-normal.
    vec4 reflection = normalize(reflect(-specularLightDirection, normVar));
    if ( diffuseCoefficient > 0 )
    {
        float spec = max(0.0, dot(normVar,reflection));
        spec = pow(spec, 128.0);
        specularLightMag = vec4(spec,spec,spec,1.0);
    }
    gl_Position = homogeniousCoordPos;
}
