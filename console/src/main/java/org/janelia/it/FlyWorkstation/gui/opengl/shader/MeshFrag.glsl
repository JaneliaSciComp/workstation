#version 150 // Highest supported on Mac 10.8.4
// pass through

in vec3 vertexColor;
in vec3 halfWayVector;
in vec3 normalDirection;

out vec4 fragColor;

const float specularPower = 20.0;
const vec3 specularColor = vec3(0.3,0.3,0.3);

void main()
{
    // fragColor = color; // fixed-function pass through
    
    float nDotH = dot(normalize(normalDirection), normalize(halfWayVector));
    float specularIntensity = pow(clamp(nDotH, 0, 1), specularPower);
    vec3 color = vertexColor + specularIntensity * specularColor;
    
    fragColor = vec4(color, 1);
}
