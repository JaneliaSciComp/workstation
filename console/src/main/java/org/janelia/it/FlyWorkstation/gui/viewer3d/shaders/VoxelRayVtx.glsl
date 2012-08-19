// Shader to simulate a deep voxel painted on a flat quad

varying vec3 cameraDirectionInVolume;

void main(void)
{
	// pass through values
    // gl_FrontColor = gl_Color;
    gl_TexCoord[0] = gl_MultiTexCoord0;
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
 
    vec4 positionInCamera = gl_ModelViewMatrix * gl_Vertex;
    vec4 cameraDirectionInCamera = vec4(-positionInCamera.xyz, 0.0);
    cameraDirectionInVolume = (gl_ModelViewMatrixInverse * cameraDirectionInCamera).xyz;
}
