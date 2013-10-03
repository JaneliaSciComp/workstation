#version 120 // max supported on Snow Leopard

uniform vec3 neuronColor = vec3(1.0, 0.7, 0.7);

void main()
{
    gl_FragColor = vec4(neuronColor, 0.5);
}
