#version 430

/**
 * Sphere imposter fragment shader.
 */



layout(location = 2) uniform mat4 projectionMatrix; // needed for proper sphere depth calculation
layout(location = 3) uniform sampler2D lightProbe;
layout(location = 4) uniform vec2 screenSize = vec2(1280, 800);

in vec3 imposterPos; // imposter geometry location, in camera frame
in float pc, c2; // pre-computed ray-casting quadratic formula linear coefficients
in vec3 center; // sphere center in camera frame
in float fragRadius; // sphere radius
in vec4 color;

out vec4 fragColor;


// methods defined in imposter_fns330.glsl
vec2 sphere_nonlinear_coeffs(vec3 pos, float pc, float c2); // sphere surface ray-casting intermediate parameters
vec3 sphere_surface_from_coeffs(vec3 pos, float pc, vec2 a2_d, out vec3 back_surface); //
vec3 light_rig(vec3 pos, vec3 normal, vec3 color); // simple hard-coded shading, for testing
float fragDepthFromEyeXyz(vec3 eyeXyz, mat4 projectionMatrix); // computes correct sphere depth-buffer value
float zNearFromProjection(mat4 projectionMatrix);
vec3 image_based_lighting(
        vec3 pos, // surface position, in camera frame
        vec3 normal, // surface normal, in camera frame
        vec3 diffuseColor, 
        vec3 reflectColor,
        sampler2D lightProbe);


void main() {
    vec2 a2_d = sphere_nonlinear_coeffs(imposterPos, pc, c2);

    vec3 s, normal;
    // fast-fail rays that miss sphere, before expensively solving exact surface location
    if (a2_d.y < 0) { // quadratic formula discriminant, (b^2 - 4ac) < 0
        // At low zoom, don't discard: It causes artifacts
        // Compute pixel size, to determine current zoom level:
        vec4 shifted1 = projectionMatrix * (vec4(center, 1) + vec4(0.1, 0, 0, 0));
        vec4 shifted2 = projectionMatrix * (vec4(center, 1) - vec4(0.1, 0, 0, 0));
        float screensPerMicrometer = 5 * abs(shifted1.x/shifted1.w - shifted2.x/shifted2.w);
        float micrometersPerPixel = 1.0 / (screensPerMicrometer * screenSize.x);
        if (micrometersPerPixel > 0.2 * fragRadius) {
            // TODO: below assumes imposter is near the front surface of the quadric
            s = imposterPos;
            normal = vec3(0, 0, 1);
            gl_FragDepth = fragDepthFromEyeXyz(s, projectionMatrix);
        }
        else {
            discard;
        }
    }
    else {
        vec3 back_surface;
        s = sphere_surface_from_coeffs(imposterPos, pc, a2_d, back_surface);
        normal = 1.0 / fragRadius * (s - center); // normalized without an additional sqrt! :)

        gl_FragDepth = fragDepthFromEyeXyz(s, projectionMatrix);

        // Near clip to reveal solid core
        if (gl_FragDepth < 0) { // Near surface is clipped by zNear
            // Show nothing if rear surface is also closer than zNear
            float back_depth = fragDepthFromEyeXyz(back_surface, projectionMatrix);
            // if (back_depth < 0) discard;
            gl_FragDepth = 0;
            s.z = zNearFromProjection(projectionMatrix); // Update clipped Z coordinate
            normal = vec3(0, 0, 1); // slice core parallel to screen
        }
    }

    // Color and shading
    vec3 reflectColor = mix(color.rgb, vec3(1,1,1), 0.5); // midway between metal and plastic.
    fragColor = vec4(
        image_based_lighting(s, normal, color.rgb, reflectColor, lightProbe),
        // light_rig(s, normal, color.rgb),
        color.a);
        // 1.0);

    // fragColor = vec4(1, 0.9, 0.9, 1.0); // for debugging
}
