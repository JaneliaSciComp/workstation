#version 120 // max supported on Snow Leopard

varying vec4 pathColor;
varying float fog;

void main()
{
    if (fog >= 1)
        discard;
    gl_FragColor = pathColor;
    gl_FragColor.a *= (1.0 - fog);
    // gl_FragColor = vec4(1.0, 1.0, 0.3, 1.0);
}
