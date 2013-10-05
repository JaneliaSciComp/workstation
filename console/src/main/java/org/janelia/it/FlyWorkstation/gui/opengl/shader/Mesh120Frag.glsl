#version 120 // Highest supported on Mac 10.6

varying vec3 vertexColor;
varying vec3 halfWayVector;
varying vec3 normalDirection;

const float specularPower = 20.0;
const vec3 specularColor = vec3(0.3,0.3,0.3);

void main()
{
    // fragColor = color; // fixed-function pass through
    
    float nDotH = dot(normalize(normalDirection), normalize(halfWayVector));
    float specularIntensity = pow(clamp(nDotH, 0, 1), specularPower);
    vec3 color = vertexColor + specularIntensity * specularColor;
    
    gl_FragColor = vec4(color, 1);
}
