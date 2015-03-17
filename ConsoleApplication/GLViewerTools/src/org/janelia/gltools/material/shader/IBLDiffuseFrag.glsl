#version 330

// Diffuse component of Image Based Lighting proof-of-concept

const vec4 diffuseColor = vec4(0.6, 0.4, 0.1, 1);
const vec4 reflectColor = 4 * vec4(0.6, 0.5, 0.3, 1);
// radius 0.49 instead of 0.50, to avoid the black edge of the light probe
const vec4 diffuseTcPos = vec4(0.25, 0.5, 0.245, -0.49); // center and radius of light probe image
const vec4 reflectTcPos = vec4(0.75, 0.5, 0.245, -0.49); // center and radius of light probe image

uniform sampler2D diffuseLightProbe;

in vec3 fragNormal;
in vec4 fragPosition;

out vec4 fragColor;

// Advanced wireframe fragment shader
void main() {
    // Diffuse
    vec3 n = normalize(fragNormal);
    // convert normal vector to position in diffuse light probe texture
    float radius = 0.50 * (-n.z + 1.0);
    vec2 direction = normalize(n.xy);
    vec2 diffuseTc = diffuseTcPos.xy + diffuseTcPos.zw * radius * direction;
    vec4 iblDiffuse = texture(diffuseLightProbe, diffuseTc);

    vec3 view = fragPosition.xyz;
    vec3 r = normalize(view - 2.0 * dot(n, view) * n); // reflection vector
    radius = 0.50 * (-r.z + 1.0);
    direction = normalize(r.xy);
    vec2 reflectTc = reflectTcPos.xy + reflectTcPos.zw * radius * direction;
    vec4 iblReflect = texture(diffuseLightProbe, reflectTc);

    fragColor = iblDiffuse * diffuseColor + iblReflect * reflectColor;
}
