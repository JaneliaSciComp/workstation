#version 430

uniform mat4 proj;
uniform mat4 view;
uniform mat4 model;

uniform vec4 dcolor;

layout (location=0) in vec3 iv;
layout (location=1) in vec3 norm;
layout (location=2) in vec4 carr;

//out vec3 N;
//out vec3 I;
//out vec4 Cs;

out vec3 diffuseColor;
out vec3 specularColor;


void main()
{

    vec3 lightPosition = vec3(4.0, 4.0, 4.0);
    vec3 lightColor = vec3(1.0, 1.0, 0.8);
    vec3 eyePosition = vec3(0.0, 0.0, 4.0);
    vec3 specular = vec3(0.2, 0.2, 0.0);
    vec3 ambient = vec3(0.2, 0.2, 0.0);
    float Kd = 0.8;

    mat4 mview = view * model;
    vec4 vertexPosition = vec4( iv.x, iv.y, iv.z, 1.0);
    mat3 normalMatrix = mat3(transpose(inverse(mview)));



    vec3 ecPosition = vec3(mview * vertexPosition);
    vec3 tnorm = normalize(normalMatrix * norm);
    vec3 lightVec = normalize(lightPosition - ecPosition);
    vec3 viewVec = normalize(eyePosition - ecPosition);
    vec3 hvec = normalize(viewVec + lightVec);

    float spec = abs(dot(hvec, tnorm));
    spec = pow(spec, 16.0);

    diffuseColor = lightColor * vec3(Kd * abs(dot(lightVec, tnorm)));
    diffuseColor = clamp(ambient + diffuseColor, 0.0, 1.0);
    specularColor = clamp( (lightColor * specular * spec), 0.0, 1.0);

    vec4 P = mview * vertexPosition;

    gl_Position = proj * P;


    // X-ray

     //Cs = vec4(dcolor.x, dcolor.y, dcolor.z, dcolor.w);
     //Cs = carr;
     //vec4 centeredIv = vec4( iv.x, iv.y, iv.z, 1.0);
     //mat4 mview = view * model;
     //vec4 P = mview * centeredIv;
     //I  = P.xyz - vec3 (0);

     //mat3 normalMatrix = mat3(transpose(inverse(mview)));
     //N  = normalMatrix * norm;
 
     //gl_Position = proj * P;
}