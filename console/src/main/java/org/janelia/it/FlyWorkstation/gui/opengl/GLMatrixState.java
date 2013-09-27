package org.janelia.it.FlyWorkstation.gui.opengl;

import org.janelia.it.FlyWorkstation.gui.opengl.GL2Adapter.MatrixMode;

public class GLMatrixState {    
    private GLMatrix projectionMatrix = new GLMatrix();
    private GLMatrix modelViewMatrix = new GLMatrix();
    
    private GLMatrix currentMatrix = modelViewMatrix;

    /**
     * Replace the current matrix with the identity matrix
     */
    void glLoadIdentity() {
        currentMatrix.glLoadIdentity();
    }
    
    /**
     * Specify which matrix is the current matrix.
     * @param mode Specifies which matrix stack is the target
                    for subsequent matrix operations.
                    Three values are accepted:
                    GL_MODELVIEW,
                    GL_PROJECTION, and
                    GL_TEXTURE.
                    The initial value is GL_MODELVIEW.
                    Additionally, if the ARB_imaging extension is supported,
                    GL_COLOR is also accepted.
     */
    void glMatrixMode(MatrixMode mode) {
        switch(mode) {
        case GL_MODELVIEW:
            currentMatrix = modelViewMatrix;
            break;
        case GL_PROJECTION:
            currentMatrix = projectionMatrix;
            break;
        default:
            currentMatrix = modelViewMatrix;
            break;
        }
    }
}
