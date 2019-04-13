
package org.janelia.geometry3d;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

/**
 * Mesh face, part of a MeshGeometry, consisting of a series of
 * vertex indices, representing a closed loop.
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class Face {
    private Color color = null;
    private Vector3 normal = null;
    private final List<Integer> vertices;

    public Face(List<Integer> vertices) {
        this.vertices = vertices;
    }
    
    public Face(Integer[] vertices) {
        this.vertices = Arrays.asList(vertices);
    }

    public Face(int[] vertices) {
        Integer[] newArray = new Integer[vertices.length];
        for (int i = 0; i < vertices.length; ++i)
            newArray[i] = vertices[i];
        this.vertices = Arrays.asList(newArray);
    }
    
    public Color getColor() {
        return color;
    }

    public Vector3 getNormal() {
        return normal;
    }
    
    public List<Integer> getVertices() {
        return vertices;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void setNormal(Vector3 normal) {
        this.normal = normal;
    }

}
