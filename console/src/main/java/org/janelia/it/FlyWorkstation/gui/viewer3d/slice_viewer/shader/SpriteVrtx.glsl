#version 120 // max supported on Snow Leopard
#extension GL_EXT_gpu_shader4 : enable

// Sometimes one anchor gets highlighted, when the mouse hovers over it.
uniform int highlightAnchorIndex = -1;

void main(void)
{
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    
    // Larger shape for hovered anchor
    gl_PointSize = 12.0;
    if (highlightAnchorIndex == gl_VertexID)
        gl_PointSize = 16.0; // No effect!
}
