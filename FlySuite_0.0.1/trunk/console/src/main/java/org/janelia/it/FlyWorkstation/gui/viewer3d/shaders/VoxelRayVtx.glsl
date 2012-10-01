// Shader to simulate a deep voxel painted on a flat quad

// The size of one voxel in micrometers: x, y, z
uniform vec3 voxelMicrometers;

varying vec3 cameraDirectionInVolume;
varying vec3 principalViewAxis;

void main(void)
{
	// pass through values
    // gl_FrontColor = gl_Color;
    gl_TexCoord[0] = gl_MultiTexCoord0;
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
 
    vec4 positionInCamera = gl_ModelViewMatrix * gl_Vertex;
    vec4 cameraDirectionInCamera = vec4(-positionInCamera.xyz, 0.0);
    cameraDirectionInVolume = (gl_ModelViewMatrixInverse * cameraDirectionInCamera).xyz;
    
    // precompute principal view axis for fragment shader,
    // so we can know orientation of imposter plane
    vec4 viewDirectionInCamera = vec4(0.0, 0.0, -1.0, 0.0);
    vec4 viewDirectionInVolume = gl_ModelViewMatrixInverse * viewDirectionInCamera;
    vec3 vabs = abs(viewDirectionInVolume.xyz);
    float vmax = vabs.x; // whoo hoo!  I guess x axis?
    vec3 paxis = vec3(1.0, 0.0, 0.0);
    if (vabs.y > vmax) { // ah! y axis is better
        paxis = vec3(0.0, 1.0, 0.0);
        vmax = vabs.y;
    }
    if (vabs.z > vmax) { // hmm, actually the z axis is best
        paxis = vec3(0.0, 0.0, 1.0);
    }
    if (dot(viewDirectionInVolume.xyz, paxis) <= 0.0) { // check for negative direction
        paxis = -paxis;
    }
    principalViewAxis = paxis * voxelMicrometers;
}
