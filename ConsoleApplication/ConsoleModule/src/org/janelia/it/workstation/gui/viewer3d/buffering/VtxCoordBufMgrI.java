/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.viewer3d.buffering;

import javax.media.opengl.GL2;
import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureMediator;

/**
 * Implement this to make something capable of establishing vertex and 
 * texture coordinates and drawing the resulting object.
 * @author fosterl
 */
public interface VtxCoordBufMgrI {

    /**
     * This method builds the buffers of vertices for both geometry and texture.  These are calculated similarly,
     * but with different ranges.  There are multiple such buffers of both types, and they are kept in arrays.
     * The arrays are indexed as follows:
     * 1. Offsets [ 0..2 ] are for positive direction.
     * 2. Offsets 0,3 are X; 1,4 are Y and 2,5 are Z.
     */
    void buildBuffers();

    /**
     * Draw the contents of the buffer as needed. Call this from another draw method.
     *
     * @param gl an openGL object as provided during draw, init, etc.
     * @param axis an X,Y, or Z
     * @param direction inwards/outwards [-1.0, 1.0]
     */
    void draw(GL2 gl, CoordinateAxis axis, double direction);

    /** This is used ONLY for non-textured rendering.  Shapes only. */
    void drawNoTex(GL2 gl, CoordinateAxis axis, double direction);

    /**
     * If statical (no repeated-upload) is used for the vertices, then this may be called to reduce
     * memory.
     */
    void dropBuffers();

    /**
     * To use vertex and coordinate data, it must first be uploaded, and enabled.  Its role must
     * be designated, and pointers need to be saved.
     *
     * @param gl for graphic-oriented operations.
     * @throws Exception thrown by any called code.
     */
    void enableBuffers(GL2 gl) throws Exception;

    void releaseBuffers(GL2 gl);

    void setCoordAttributeLocations(int vertexAttributeLoc, int texCoordAttributeLoc);

    void setTextureMediator(TextureMediator textureMediator);
    
}
