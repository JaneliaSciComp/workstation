package org.janelia.it.FlyWorkstation.gui.opengl;

import java.util.Stack;

import org.janelia.it.FlyWorkstation.gui.opengl.GL2Adapter.MatrixMode;

// Replacement for implicit transformation graph from OpenGL <= 3.0
// for use in later versions of OpenGL
public class GLMatrixState {    
    private Stack<GLMatrix> projectionMatrix = new Stack<GLMatrix>();
    private Stack<GLMatrix> modelViewMatrix = new Stack<GLMatrix>();
    
    private Stack<GLMatrix> currentMatrix = modelViewMatrix;

    public GLMatrixState() {
        // begin with at least one matrix on the stack
        projectionMatrix.push(new GLMatrix());
        modelViewMatrix.push(new GLMatrix());
    }
    
    public GLMatrix getCurrentMatrix() {
        return currentMatrix.peek();
    }

    /**
     * Replace the current matrix with the identity matrix
     */
    void glLoadIdentity() {
        currentMatrix.peek().glLoadIdentity();
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

    public float[] getModelViewMatrix() {
        return modelViewMatrix.peek().getFloatArray();
    }

    public float[] getProjectionMatrix() {
        return projectionMatrix.peek().getFloatArray();
    }

}
