
package org.janelia.geometry3d;

import java.awt.Color;

/**
 * Triangle consisting of three vertex indices, forming part of a MeshGeometry.
 * @author brunsc
 */
public class Triangle {
    final private int[] indices = new int[3];
    private Vector3 normal = null;
    private Color color = null;

    public Triangle(int a, int b, int c) {
        indices[0] = a;
        indices[1] = b;
        indices[2] = c;
    }

    public int[] asArray() {
        return indices;
    }

    public int getVertexIndex(int index) {
        return indices[index];
    }

    public Color getColor() {
        return color;
    }

    public Vector3 getNormal() {
        return normal;
    }

    public void setColor(Color color) {
        this.color = color;
    }
    
    void setNormal(Vector3 normal) {
        this.normal = normal;
    }
    
}
