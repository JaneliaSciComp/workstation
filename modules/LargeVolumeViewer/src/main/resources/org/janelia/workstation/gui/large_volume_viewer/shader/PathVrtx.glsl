#version 120 // max supported on Snow Leopard

// pass-through vertex shader intended to recapitulate the 
// fixed-function OpenGL pipeline

uniform vec3 baseColor;
uniform float zThickness = 100.0;
uniform vec3 focus = vec3(0,0,0);

varying vec4 pathColor;
varying float fog;

void main(void)
{
    // combine base color (black edge or white middle) with skeleton color (gl_Color)
    pathColor = gl_Color * vec4(baseColor, 1);
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;

    // Points fade away above and below current Z position
    // Note that gl_ModelViewMatrix includes scale pixelsPerSceneUnit
    vec4 vertexM = gl_ModelViewMatrix * gl_Vertex;
    vec4 focusM = gl_ModelViewMatrix * vec4(focus, 1);
    float relZ = 2.0 * (vertexM.z - focusM.z) / zThickness; // range -1:1
    fog = relZ;    
}
