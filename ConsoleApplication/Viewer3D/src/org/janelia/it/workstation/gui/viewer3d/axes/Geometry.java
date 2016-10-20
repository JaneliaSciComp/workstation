/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.viewer3d.axes;

/**
 * Convenience class to carry around all numbers associated with some thing to
 * draw.
 *
 * A few subtle points about geometry: 1. indices point at vertices. 2. each
 * vertex consists of three coordinates, here: 0th is x, 1st is y, 2nd is z 3.
 * there are always 3x coords as vertices. 4. a count in an index (1,2,3...)
 * corresponds to a by-3 in coords (3,6,9...)
 *
 * Since this class keeps all its data in one buffer for vertices and one for
 * indices, it is important to keep this in mind when incrementing the count.
 * The value passed in as the next index is actually going to be the next
 * _vertex_ after all vertices of the previous displayable 'shape'.
 * 
 * @author fosterl
 */
public class Geometry {

    private float[] vertices;
    private int[] indices;

    public Geometry(float[] vertices, int[] indices) {
        this.vertices = vertices;
        this.indices = indices;
    }

    public float[] getCoords() {
        return vertices;
    }

    public int[] getIndices() {
        return indices;
    }

    public int getVertexCount() {
        return getCoords().length / 3;
    }

}
