#version 120 // max supported on Snow Leopard

varying vec4 outlineColor;

void main()
{
    gl_FragColor = outlineColor;
    // gl_FragColor = vec4(1.0, 1.0, 0.3, 1.0);
}
