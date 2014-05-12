#version 150 // Highest supported on Mac 10.8.4

uniform mat4 projection;
uniform mat4 modelView;

in vec4 vertex;
in vec3 normal;

out vec3 halfWayVector;
out vec3 vertexColor;
out vec3 normalDirection;

struct Light
{
    vec4 position;
    vec3 color;
};

const Light light0 = Light(
    vec4(1,1,1,0),
    vec3(1,1,1));


struct Material
{
    vec3 ambientColor;
    vec3 diffuseColor;
    vec3 specularColor;
    float specularPower;
};

const Material material = Material(
    vec3(0.1, 0.1, 0.1),
    vec3(0.6, 0.6, 0.2),
    vec3(0.3, 0.3, 0.3),
    20.0);


vec3 calcDiffuseColor(vec3 lightDirection, vec3 normalDirection, 
        vec3 lightColor, vec3 materialColor)
{
    float diffuseIntensity = clamp(dot(lightDirection, normalDirection), 0.0, 1.0);
    return diffuseIntensity * lightColor * materialColor;
}

void main(void) {
    gl_Position = modelView * vertex;
    
    // TODO - refactor lighting calculation into functions...
    vec3 lightDirection = normalize(light0.position.xyz - light0.position.w * gl_Position.xyz);
    normalDirection = normalize( (modelView * vec4(normal, 0)).xyz );
    vec3 viewDirection = normalize(-gl_Position.xyz);
    halfWayVector = normalize(viewDirection + lightDirection);
    vertexColor = vec3(0,0,0);
    vertexColor += calcDiffuseColor(lightDirection, normalDirection, 
            light0.color, material.diffuseColor);
    
    gl_Position = projection * gl_Position;
}
