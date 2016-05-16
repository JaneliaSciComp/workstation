#version 330

// file: TransparentEnveloperFrag.glsl
// Fragment shader for TransparentEnvelope material
// Shows a transparent edge-enhanced mesh for 3D brain compartment surfaces

// TODO: expose lighting parameters as uniform variables
// TODO: permit per-vertex lighting

in vec3 fragNormal;
in vec4 fragPosition;

uniform vec3 diffuseColor = vec3(1.0, 0.8, 0.5);

const vec4 lightPosition = vec4(-10, 20, 6, 0);
const vec3 lightColor = vec3(1, 1, 1);
const float ambientScale = 0.5; // unshaded component
const float diffuseScale = 0.3; // shaded component
const float specularScale = 0.5; // shininess component
const float specularPower = 20.0;
vec3 specularColor = lightColor;
vec3 diffuseColor1 = 
    // vec3(1, 0, 0); // for testing
    diffuseColor;
vec3 ambientColor = diffuseColor1;


out vec4 fragColor;

void main(void)
{
  // Use Lambertian lighting model for a bit of shading
  vec3 n = normalize(fragNormal);
  vec3 camPos = fragPosition.xyz/fragPosition.w;
  // flip normals of back faces
  if (dot(n, camPos) < 0) {
    n = -n;
  }
  vec3 L = lightPosition.xyz - lightPosition.w * camPos;
  L = normalize(L);
  vec3 ambient = ambientScale * ambientColor;
  vec3 diffuse = diffuseScale * max(dot(n, L), 0) * diffuseColor1;
  // specular http://www.opengl-tutorial.org/beginners-tutorials/tutorial-8-basic-shading/
  vec3 eyeDirection = normalize(camPos);
  vec3 specReflect = reflect(L, n);
  float cosAlpha = max(0, dot(eyeDirection, specReflect));
  float specularIntensity = pow(cosAlpha, specularPower);
  vec3 specular = specularScale * specularIntensity * specularColor;

  // Only show edges viewed obliquely, revealing a transparent outline
  const float angleCosineCutoff = 0.8;
  const float maxOpacity = 0.70;
  float straight_on_ness = abs(dot(n, eyeDirection)) / angleCosineCutoff;
  straight_on_ness = clamp(straight_on_ness, 0, 1);
  if (straight_on_ness >= 1.0) discard; // Don't even draw viewed-face-on geometry
  float edginess = maxOpacity * (1.0 - straight_on_ness);
  fragColor = vec4(diffuse + ambient + specular, edginess);
}
