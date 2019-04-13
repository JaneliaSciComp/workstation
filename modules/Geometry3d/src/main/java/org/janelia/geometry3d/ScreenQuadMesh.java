package org.janelia.geometry3d;

import java.awt.Color;

/**
 * Quadrilateral mesh used to cover the whole screen.
 * Suitable for either per-vertex coloring, or texture mapping.
 * @author Christopher Bruns
 */
public class ScreenQuadMesh extends MeshGeometry 
{
    
    public ScreenQuadMesh() {
        initialize(Color.BLACK);
    }

    public ScreenQuadMesh(Color color) {
        initialize(color);
    }

    public ScreenQuadMesh(Color topColor, Color bottomColor) {
        initialize(topColor);
        setBottomColor(bottomColor);
    }

    private void initialize(Color color) {
        float red = color.getRed()/255.0f;
        float green = color.getGreen()/255.0f;
        float blue = color.getBlue()/255.0f;
        float alpha = color.getAlpha()/255.0f;
        
        // Use normalized device coordinates, so no matrix multiplies will be required in shader.
        Vertex v;
        v = addVertex(-1, -1, 0.5f);
        v.setAttribute("color", new Vector4(red, green, blue, alpha));
        v.setAttribute("texCoord", new Vector2(0, 0));

        v = addVertex( 1, -1, 0.5f);
        v.setAttribute("color", new Vector4(red, green, blue, alpha));
        v.setAttribute("texCoord", new Vector2(1, 0));
        
        v = addVertex( 1,  1, 0.5f);
        v.setAttribute("color", new Vector4(red, green, blue, alpha));
        v.setAttribute("texCoord", new Vector2(1, 1));
        
        v = addVertex(-1,  1, 0.5f);
        v.setAttribute("color", new Vector4(red, green, blue, alpha));
        v.setAttribute("texCoord", new Vector2(0, 1));
        
        addFace(new int[] {0, 1, 2, 3});
    }
    
    public void setBottomColor(Color color) {
        for (int ix : new int[] {0, 1}) {
            Vertex vtx = vertices.get(ix);
            Vector4 col = (Vector4)vtx.getVectorAttribute("color");
            col.set(0, color.getRed()/255.0f);
            col.set(1, color.getGreen()/255.0f);
            col.set(2, color.getBlue()/255.0f);
            col.set(3, color.getAlpha()/255.0f);
        }
        setChanged();        
    }
    
    public void setTopColor(Color color) {
        for (int ix : new int[] {2, 3}) {
            Vertex vtx = vertices.get(ix);
            Vector4 col = (Vector4)vtx.getVectorAttribute("color");
            col.set(0, color.getRed()/255.0f);
            col.set(1, color.getGreen()/255.0f);
            col.set(2, color.getBlue()/255.0f);
            col.set(3, color.getAlpha()/255.0f);
        }
        setChanged();
    }
    
    public void setColor(Color color) 
    {
        float red = color.getRed()/255.0f;
        float green = color.getGreen()/255.0f;
        float blue = color.getBlue()/255.0f;
        float alpha = color.getAlpha()/255.0f;
        for (Vertex v : vertices) {
            Vector4 c = (Vector4)v.getVectorAttribute("color");
            c.set(0, red);
            c.set(1, green);
            c.set(2, blue);
            c.set(3, alpha);
        }
        setChanged();
    }
}
