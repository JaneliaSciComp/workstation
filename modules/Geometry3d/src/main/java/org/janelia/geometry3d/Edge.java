
package org.janelia.geometry3d;

import java.util.Arrays;

/**
 * Represents one edge of a Geometry mesh.
 * 
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class Edge {
    final private int[] indices = new int[2];

    public Edge(int p1, int p2) {
        // Always store in increasing order, for hash/equals uniqueness
        if (p1 < p2) {
            indices[0] = p1;
            indices[1] = p2;
        }
        else {
            indices[1] = p1;
            indices[0] = p2;            
        }
    }

    public int[] asArray() {
        return indices;
    }

    public int get(int index) {
        return indices[index];
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + Arrays.hashCode(this.indices);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Edge other = (Edge) obj;
        if (!Arrays.equals(this.indices, other.indices)) {
            return false;
        }
        return true;
    }
    
    
}
