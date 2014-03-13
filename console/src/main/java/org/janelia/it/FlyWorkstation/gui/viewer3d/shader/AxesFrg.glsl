// Fragment shader for all filtering applied by volume brick.
#version 120
uniform sampler3D signalTexture;
uniform vec4 color;

void main()
{
    gl_FragColor = color;
}

