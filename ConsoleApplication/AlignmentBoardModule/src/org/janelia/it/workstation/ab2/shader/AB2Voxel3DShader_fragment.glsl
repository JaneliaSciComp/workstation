#version 450

in vec4 colorg;
out vec4 color;

uniform ivec4 xyBounds;

void main() {
    color = colorg;
    if (gl_FragCoord.x < xyBounds.x ||
        gl_FragCoord.y < xyBounds.y ||
        gl_FragCoord.x > xyBounds.z ||
        gl_FragCoord.y > xyBounds.w) {
       discard;
    }
}
