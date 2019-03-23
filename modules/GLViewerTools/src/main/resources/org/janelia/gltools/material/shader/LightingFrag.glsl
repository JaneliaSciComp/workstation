#version 330

uniform sampler2D upstreamImage;

in vec2 screenCoord; // from RenderPassVrtx shader

out vec4 fragColor;

// TODO - use image based lighting
const vec3 lightPos = normalize(vec3(-0.5, 2, 0.3));

void main() {
    vec4 c = texture(upstreamImage, screenCoord);
    if (c.a <= 0) 
        discard;
    vec3 normal = normalize(c.rgb);

    vec3 diffuseColor = vec3(0.1, 1.0, 0.1);
    float diffuseCoefficient = max(0, dot(normal, lightPos));
    vec3 diffuse = diffuseColor * diffuseCoefficient;

    float ambientCoefficient = 0.07;
    vec3 ambient = diffuseColor * ambientCoefficient;

    vec3 incidenceVector = -lightPos; //a unit vector
    vec3 reflectionVector = reflect(incidenceVector, normal); //also a unit vector
    vec3 surfaceToCamera = vec3(0, 0, 1); //also a unit vector
    float cosAngle = max(0.0, dot(surfaceToCamera, reflectionVector));
    float specularCoefficient = pow(cosAngle, 20);
    vec3 specularColor = vec3(1, 1, 1);
    vec3 specular = specularCoefficient * specularColor * 2;

    fragColor = vec4(diffuse + ambient + specular, c.a);
}

