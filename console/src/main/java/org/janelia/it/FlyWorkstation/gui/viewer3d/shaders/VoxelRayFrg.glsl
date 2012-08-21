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

varying vec3 principalViewAxis; // 

void main()
{
    // Compute direction of the eye ray, which is required to ray trace the voxel
    vec3 eyeVec = normalize(cameraDirectionInVolume); // points TOWARD eye from this fragment
    // We want to compute anything we need from ray parameter, t
    // Ray parameter "t" is proportional to ray length, and thus to voxel thickness.
    vec3 dPosDt = eyeVec; // dp/dt
    vec3 dTexDPos = vec3(1.0,1.0,1.0) / textureVoxels / voxelMicrometers; // dTexCoord/dp
    vec3 dTexDt = dPosDt * dTexDPos; // dTexCoord/dt

    const float minRayStep = 0.02; // To keep things moving forward a finite amount
    // Tracing ray proceeds from -1/2 to +1/2 voxel depth in principal viewing axis direction
    float tMin = 0.50 * dot(principalViewAxis, (vec3(1.0, 1.0, 1.0)/eyeVec)) + minRayStep;
    float tMax = -tMin;
    vec4 color = vec4(0.0, 0.0, 0.0, 0.0); // We will build up fragment color from nothing.
    // Which three of the six voxel bounding planes might a view ray cross?
    vec3 planes = ceil(eyeVec * 0.99); // each component of "planes" is now either 0.0 or 1.0
    planes = planes * voxelMicrometers; // scale the 1.0 to actual voxel dimensions
    // Trace ray forward in a sequence of steps, each of which stops
    // at a voxel boundary.
    // Ray parameter "t" is proportional to ray length, and thus to voxel thickness.
    float t = tMin; // view ray parameter, zero at default midplane fragment
    while (t < tMax) { // Up to five steps, mostly one or two.
        // Trace the view ray forward, toward the eye, until
        // we hit a voxel boundary
        vec3 textureCoordinate = gl_TexCoord[0].xyz + dTexDt * t;
        // Compute p, the fragment coordinate in the current voxel reference frame
        vec3 p = fract(textureCoordinate * textureVoxels) * voxelMicrometers;
        // Solve three ray-tracing equations in one vec3:
        vec3 distances = (planes - p) / eyeVec; // distance to each of three voxel boundaries
        // The shortest distance is the closest voxel boundary
        // -minRayStep to avoid texture artifacts at voxel boundaries
        float thickness = min(distances.x, min(distances.y, distances.z)); // This is how much voxel meat we are peering through.
        // float dT = thickness + minRayStep;
        // actually use a texture coordinate halfway through the computed thickness,  
        // to work best with different texture interpolation methods. 
        vec4 tc = texture3D(volumeTexture, textureCoordinate + 0.5 * thickness * dTexDt);
        // Compute alpha opacity from thickness using formula Cami and I derived.
        float a0 = tc.a;
        float ta0 = thickness * a0;
        float alpha = ta0 / (1.0 - a0 + ta0); // 4 flops total.  No pow() required.
        alpha = clamp(alpha, 0.0, 1.0);
        float backAlpha = color.a * (1.0 - alpha); // From previous layers
        if (alpha > 0.0) {
            // un-pre-multiply alpha component
            tc /= tc.a;
            float ratio = alpha / (alpha + backAlpha);
            color = mix(color, tc, ratio);
            color.a = alpha + backAlpha;
        }
        t += thickness + minRayStep; // step into the next voxel
    }

    // re-pre-multiply alpha component
    color.rgb *= color.a;
    gl_FragColor = color;
    
    // Debugging colorizations below
    // gl_FragColor = vec4(1.0, 0.0, 0.0, 0.5);
    // gl_FragColor = vec4(0.5 * (principalViewAxis + vec3(1.0,1.0,1.0)), 0.5);
    // float c = tMin + 0.5;
    // gl_FragColor = vec4(c,c,c,0.5);
}

