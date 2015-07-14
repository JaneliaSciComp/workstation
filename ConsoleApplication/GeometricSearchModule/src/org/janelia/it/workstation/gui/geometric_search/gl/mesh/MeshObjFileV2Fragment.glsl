#version 330

out vec4 color;

// vertex to fragment shader io
in vec3 N;
in vec3 I;
in vec4 Cs;

// globals
//uniform float edgefalloff;
//uniform float intensity;
//uniform float ambient;

// entry point
void main()
{
    float edgefalloff=1.0;
    float intensity=0.5;
    float ambient=0.01;

    float opac = dot(normalize(-N), normalize(-I));
    opac = abs(opac);
    opac = ambient + intensity*(1.0-pow(opac, edgefalloff));
    color =  opac * Cs;
    color.a = opac;
}