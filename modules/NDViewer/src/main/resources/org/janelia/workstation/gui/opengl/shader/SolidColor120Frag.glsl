#version 120 // Highest supported on Mac 10.6

uniform vec4 color = vec4(0,1,0,1);

void main()
{
    gl_FragColor = color;
}
