#version 120 // max supported on Snow Leopard
#extension GL_EXT_gpu_shader4 : enable // to get gl_VertexID

// Sometimes one anchor gets highlighted, when the mouse hovers over it.
uniform int highlightAnchorIndex = -1;
uniform int parentAnchorIndex = -1;
uniform float zThickness = 100.0;
uniform vec3 focus = vec3(0,0,0);

varying vec3 anchorColor;
varying float fog;
varying float isParent;

void main(void)
{
    // Standard view transformation
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    
    // Set color from skeleton
    anchorColor = gl_Color.rgb;

    // Points fade away above and below current Z position
    // Note that gl_ModelViewMatrix includes scale pixelsPerSceneUnit
    vec4 vertexM = gl_ModelViewMatrix * gl_Vertex;
    vec4 focusM = gl_ModelViewMatrix * vec4(focus, 1);
    float relZ = 2.0 * (vertexM.z - focusM.z) / zThickness; // range -1:1
    fog = min(1.0, abs(relZ));
    
    // smaller points are further away; bigger ones closer
    gl_PointSize = 12.0 - 5.0 * relZ; 
    gl_PointSize = max(4.0, gl_PointSize);

    // Larger shape when mouse is over anchor
    if (highlightAnchorIndex == gl_VertexID)
        gl_PointSize = 1.3 * gl_PointSize;
        
    // Different graphic for current parent anchor node
    isParent = 0;
    if (parentAnchorIndex == gl_VertexID)
        isParent = 1;
}
