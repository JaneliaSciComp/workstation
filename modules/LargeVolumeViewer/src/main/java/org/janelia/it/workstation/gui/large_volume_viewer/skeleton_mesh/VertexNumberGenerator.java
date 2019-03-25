/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh;

/**
 * Acts as a source of vertex numbers, so that their management is centralized,
 * and independent of their users.
 *
 * @author fosterl
 */
public class VertexNumberGenerator {
    int vertexNumber = 0;
    public int allocateVertexNumber() {
        return vertexNumber ++;
    }
    
    public boolean hasVertices() {
        return vertexNumber > 0;
    }
    
    /**
     * Report latest vertex that has been allocated.
     * 
     * @return current vertex, without updating it.
     */
    public int getCurrentVertex() {
        return vertexNumber;
    }
}
