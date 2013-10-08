#version 120 // Highest supported on Mac 10.6

attribute vec4 gl_Vertex;

void main(void) {
    gl_Position = gl_Vertex;
}
