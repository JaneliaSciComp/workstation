/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.viewer3d.mesh.actor;

import javax.media.opengl.GL2GL3;

/**
 * Implement this to take on capability of pushing buffers to GPU.
 *
 * @author fosterl
 */
public interface BufferUploader {
    void uploadBuffers (GL2GL3 gl);
    /**
     * @return the vtxAttribBufferHandle
     */
    int getVtxAttribBufferHandle();

    /**
     * @return the inxBufferHandle
     */
    int getInxBufferHandle();

    /**
     * @return the indexCount
     */
    int getIndexCount();

}
