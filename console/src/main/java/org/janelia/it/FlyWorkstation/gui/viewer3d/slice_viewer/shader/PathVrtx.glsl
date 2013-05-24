#version 120 // max supported on Snow Leopard

// pass-through vertex shader intended to recapitulate the 
// fixed-function OpenGL pipeline

uniform vec3 baseColor;
uniform float zThickness = 100.0;
uniform float focusZ = 0.0;

varying vec4 pathColor;
varying float fog;

void main(void)
{
    // combine base color (black edge or white middle) with skeleton color (gl_Color)
    pathColor = gl_Color * vec4(baseColor, 1);
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;

    float relZ = (gl_Vertex.z - focusZ) * 0.5 / zThickness; // range -1:1
    fog = min(1.0, abs(relZ));    
}
