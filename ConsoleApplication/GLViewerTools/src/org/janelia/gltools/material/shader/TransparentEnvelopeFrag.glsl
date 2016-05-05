#version 330

// file: TransparentEnveloperFrag.glsl
// Fragment shader for TransparentEnvelope material
// Shows a transparent edge-enhanced mesh for 3D brain compartment surfaces

// TODO: expose lighting parameters as uniform variables
// TODO: permit per-vertex lighting

in vec3 fragNormal;
in vec4 fragPosition;

uniform vec3 diffuseColor = vec3(1.0, 0.8, 0.5);

const vec4 lightPosition = vec4(10, 10, 10, 1);
const vec3 lightColor = vec3(1, 1, 1);
const float ambientScale = 0.6; // unshaded component
const float diffuseScale = 0.4; // shaded component
vec3 ambientColor = diffuseColor;

out vec4 fragColor;

void main(void)
{
  // Use Lambertian lighting model for a bit of shading
  vec3 n = normalize(fragNormal);
  vec3 camPos = fragPosition.xyz/fragPosition.w;
  vec3 L = lightPosition.xyz - lightPosition.w * camPos;
  L = normalize(L);
  vec3 ambient = ambientScale * ambientColor;
  vec3 diffuse = diffuseScale * max(dot(n, L), 0) * diffuseColor;

  // Only show edges viewed obliquely, revealing a transparent outline
  float angleCosineCutoff = 0.5;
  float straight_on_ness = abs(dot(n, normalize(camPos))) / angleCosineCutoff;
  if (straight_on_ness >= 1) 
      discard; // Don't even draw viewed-face-on geometry
  float edginess = 1.0 - straight_on_ness;
  fragColor = vec4(diffuse + ambient, edginess);
}
