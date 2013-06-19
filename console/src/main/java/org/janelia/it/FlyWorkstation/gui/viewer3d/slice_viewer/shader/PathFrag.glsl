#version 120 // max supported on Snow Leopard

varying vec4 pathColor;
varying float fog;

void main()
{
    // Postpone abs() call to here in fragment shader, because
    // abs() is not linear. Otherwise clipped middle sections of skeleton
    // edges are not shown.
    float absFog = abs(fog);
    if (absFog >= 1)
        discard;
    gl_FragColor = pathColor;
    gl_FragColor.a *= (1.0 - absFog);
    // gl_FragColor = vec4(1.0, 1.0, 0.3, 1.0); // for debugging
}
