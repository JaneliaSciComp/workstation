// Shader to simulate a deep voxel painted on a flat quadrilateral

// RGBA 3d texture of color signal data
uniform sampler3D volumeTexture;

// The number of voxels in the 3d texture in each direction x,y,z
uniform vec3 textureVoxels; // for computing voxel boundaries

// The size of one voxel in micrometers: x, y, z
uniform vec3 voxelMicrometers;

// Location of this fragment in the reference frame of the VolumeBrick:
//   origin is at volume corner
//   X, Y, Z along volume axes
//   units are micrometers
varying vec3 cameraDirectionInVolume;

void main()
{
	// pass through color of central voxel
    gl_FragColor = texture3D(volumeTexture, gl_TexCoord[0].xyz);
    
    // Phase I - only compute the contribution of this central voxel
    // (later we will visit adjacent voxels to explore the whole ray)
    // Phase I - only compute the contribution of the front half of this voxel.
    // (later we will go forward and back, or perhaps forward from the very back plane.

    // Modify alpha by ray traced voxel thickness //
    
    // Compute view vector and fragment position in voxel
    vec3 eyeVec = normalize(cameraDirectionInVolume); // points TOWARD eye from this fragment
    // Transform point into unit voxel coordinates
    vec3 uvc = fract(gl_TexCoord[0].xyz * textureVoxels);
    // Scale by voxel size to get units in micrometers
    vec3 p = uvc * voxelMicrometers;

	// RAY TRACING    
    // Three axis-aligned plane equations define the voxel surface
    // in the general direction of the viewer.  Our ray stops
    // when it hits the CLOSEST of these three planes.  The
    // ray tracing equations for axis aligned planes are so
    // simple, we will pack all three into single vec3s.  Sorry.
    vec3 planes = ceil(eyeVec * 0.99); // each component of "planes" is now either 0.0 or 1.0
    planes = planes * voxelMicrometers; // scale the 1.0 to actual voxel dimensions
    // Here it is! Solve three ray-tracing equations at once:
    vec3 distances = (planes - p) / eyeVec; // distance to each of three voxel boundaries
    float thickness = min(distances.x, min(distances.y, distances.z)); // This is how much voxel meat we are peering through.

    // Now do the back side of the voxel
    vec3 backPlanes = ceil(-eyeVec * 0.99);
    backPlanes *= voxelMicrometers;
    vec3 backDistances = (backPlanes - p) / eyeVec; // distance to each of three voxel boundaries
    vec3 bd = -backDistances;
    float backThickness = min(bd.x, min(bd.y, bd.z));
    thickness += backThickness;

    // Convert thickness to opacity (alpha).
    // This is not so simple.
    // Cami helped me derive a formula with the desired shape.
    // The "real" formula is alpha(thickness) = a0 ^ (t0/thickness)
    //   where t0 is the standard thickness (1.0 micrometers),
    //   a0 is the normalized opacity (per micrometer)
    // Our formula satisfies these requirements:
    //   alpha(0) = 0.0
    //   alpha(t0) = a0
    //   alpha(infinity) = 1.0
    //   alpha(t) smooth and continuous
    //   no trancendental functions
    float a0 = gl_FragColor.a;
    // const float t0 = 1.0;
    float ta0 = thickness * a0;
    float alpha = ta0 / (1.0 - a0 + ta0); // 4 flops total.  No pow() required.
    gl_FragColor.a = alpha;
    
    // debugging only below
    // gl_FragColor.a = a0;
    // gl_FragColor.a = thickness;
    // gl_FragColor.rgb = 0.5 * (distances + vec3(1.0, 1.0 ,1.0));
    // gl_FragColor.rgb = planes;
    // gl_FragColor.a = 1.0;
}
