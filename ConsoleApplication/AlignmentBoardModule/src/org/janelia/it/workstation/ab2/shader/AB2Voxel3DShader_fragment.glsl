#version 410

in vec4 colorg;
out vec4 color;

uniform ivec4 xyBounds;
uniform vec2 irange;

void main() {
    if (gl_FragCoord.x < xyBounds.x ||
        gl_FragCoord.y < xyBounds.y ||
        gl_FragCoord.x > xyBounds.z ||
        gl_FragCoord.y > xyBounds.w) {
       discard;
    }
    color.r=clamp((colorg.r-irange.x)/(irange.y-irange.x), 0.0, 1.0);
    color.g=clamp((colorg.g-irange.x)/(irange.y-irange.x), 0.0, 1.0);
    color.b=clamp((colorg.b-irange.x)/(irange.y-irange.x), 0.0, 1.0);
    color.a=colorg.a;
}

