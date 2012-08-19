// Shader to simulate a deep voxel painted on a flat quad

varying vec3 positionInCamera; // for ray tracing view vector

void main(void)
{
	// pass through values
    // gl_FrontColor = gl_Color;
    gl_TexCoord[0] = gl_MultiTexCoord0;
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;

	// send info needed by fragment shader for ray tracing
    positionInCamera = gl_Position.xyz * 1.0 / gl_Position.w;
}
