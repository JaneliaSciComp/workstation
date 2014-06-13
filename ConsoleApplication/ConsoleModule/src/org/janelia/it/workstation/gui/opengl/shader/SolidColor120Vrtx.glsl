#version 120 // Highest supported on Mac 10.6

attribute vec4 position;

void main(void) {
    gl_Position = position;
}
