#version 120 // max supported on Snow Leopard

// Sometimes one anchor gets highlighted, when the mouse hovers over it.
uniform int highlightAnchorIndex = -1;

void main(void)
{
    // gl_PointSize = 8.0;
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    
    /*
    // Larger shape for hovered anchor
    if (highlightAnchorIndex == gl_VertexID)
        gl_PointSize = 2 * gl_PointSize;
        */
}
