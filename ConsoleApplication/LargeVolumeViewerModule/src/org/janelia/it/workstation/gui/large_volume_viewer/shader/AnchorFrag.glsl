#version 120 // max supported on Snow Leopard

uniform sampler2D anchorTexture;
uniform sampler2D parentAnchorTexture;
uniform int smallParentImage = 1;

varying vec3 anchorColor;
varying float fog;
varying float isParent;

void main ( )
{
    // No point shown outside of Z thickness range
    if (fog >= 1)
        discard;

    // Choose correct texture for this point
    vec4 c;
    if (isParent <= 0)
        c = texture2D(anchorTexture, gl_PointCoord);
    else
        // Different graphic for parent node
        c = texture2D(parentAnchorTexture, gl_PointCoord);

    // Convolute with skeleton color
    if (smallParentImage > 0) {
        c.rgb *= anchorColor.rgb; // modulate with skeleton color
        // Fade away at distant Z
        c.a *= (1.0 - fog);
    }
    else {
        if (c.r < 0.1  &&  c.g < 0.1  &&  c.b < 0.1)
            discard;
    }
    
    // Set final color    
    gl_FragColor = c;
}
