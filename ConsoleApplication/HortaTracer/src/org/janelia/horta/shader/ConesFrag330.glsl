#version 330

/**
 * Sphere imposter fragment shader.
 */

uniform vec4 color = vec4(0, 1, 0, 1); // one color for all spheres
uniform mat4 projectionMatrix; // needed for proper sphere depth calculation
uniform sampler2D lightProbe;


in float fragRadius; // average radius of cone
in vec3 center; // center of cone, in camera frame
in float taper; // change in radius per distance along cone axis
in vec3 halfAxis;
// the *linear* coefficients of the ray-tracing quadratic formula can be computed per-vertex, rather than per fragment.
in float tAP; // cone ray-casting quadratic-formula linear (actually constant) coefficient
in float qe_c; // cone ray-casting quadratic-formula linear coefficient
in float qe_half_b; // cone ray-casting quadratic-formula linear coefficient
in vec3 qe_undot_half_a; // cone ray-casting quadratic-formula linear coefficient
in vec3 imposterPos; // location of imposter bounding geometry, in camera frame
in float halfConeLength;
in vec3 aHat;
in float normalScale;


out vec4 fragColor;


// forward declaraion of methods defined in imposter_fns330.glsl
void cone_nonlinear_coeffs(in float tAP, in float qe_c, in float qe_half_b, in vec3 qe_undot_half_a,
    out float qe_half_a, out float discriminant);
vec3 cone_surface_from_coeffs(in vec3 pos, in float qe_half_b, in float qe_half_a, in float discriminant);
vec3 light_rig(vec3 pos, vec3 normal, vec3 surface_color);
float fragDepthFromEyeXyz(vec3 eyeXyz, mat4 projectionMatrix);
vec3 image_based_lighting(
        vec3 pos, // surface position, in camera frame
        vec3 normal, // surface normal, in camera frame
        vec3 diffuseColor, 
        vec3 reflectColor,
        sampler2D lightProbe);


void main() {
    // set up quadratic formula to solve cone ray-casting equation
    float qe_half_a, discriminant;
    cone_nonlinear_coeffs(tAP, qe_c, qe_half_b, qe_undot_half_a, qe_half_a, discriminant);
    if (discriminant < 0)
        discard; // not on cone

    // Compute projected surface of cone
    vec3 s = cone_surface_from_coeffs(imposterPos, qe_half_b, qe_half_a, discriminant);
    vec3 cs = s - center;
    
    // Truncate cone geometry to prescribed ends
    if ( abs(dot(cs, aHat)) > halfConeLength ) 
        discard;

    // Compute surface normal vector, for shading
    vec3 n1 = normalize( cs - dot(cs, aHat)*aHat );
    vec3 normal = normalScale * (n1 + taper * aHat);

    // illuminate the cone surface
    vec3 reflectColor = mix(color.rgb, vec3(1,1,1), 0.5); // midway between metal and plastic.
    fragColor = vec4(
        image_based_lighting(s, normal, color.rgb, reflectColor, lightProbe),
        // light_rig(s, normal, color.rgb),
        color.a);

    // Put computed cone surface Z depth into depth buffer
    gl_FragDepth = fragDepthFromEyeXyz(s, projectionMatrix);
}
