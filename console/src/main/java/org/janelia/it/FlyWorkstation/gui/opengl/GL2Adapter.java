package org.janelia.it.FlyWorkstation.gui.opengl;

import javax.media.opengl.GL2GL3;

public interface GL2Adapter {
    public static enum MatrixMode {
        GL_MODELVIEW,
        GL_PROJECTION,
    };
    
    /**
     * To support common methods between GL2 and GL3
     * @return
     */
    GL2GL3 getGL2GL3();

    /**
     * Replace the current matrix with the identity matrix
     */
    void glLoadIdentity();
    
    
    void glMatrixMode(MatrixMode mode);

}
