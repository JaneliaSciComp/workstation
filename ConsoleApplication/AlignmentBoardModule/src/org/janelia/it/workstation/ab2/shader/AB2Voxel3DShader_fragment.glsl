#version 450

in vec4 colorg;
out vec4 color;

void main() {
    color = colorg;
    if (gl_FragCoord.x > 100 && gl_FragCoord.x < 200) {
       discard;
    }
}
