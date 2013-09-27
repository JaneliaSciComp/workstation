package org.janelia.it.FlyWorkstation.gui.opengl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;

public class GL2AdapterFactory {
    public static GL2Adapter createGL2Adapter(GLAutoDrawable glDrawable) {
        GL gl = glDrawable.getGL();
        if (gl.isGL3())
            return new GL3GL2Adapter(gl.getGL3());
        if (gl.isGL2())
            return new GL2GL2Adapter(gl.getGL2());
        return null;
    }
    
    static class GL2GL2Adapter implements GL2Adapter {
        private GL2 gl;
        
        public GL2GL2Adapter(GL2 gl) {
            this.gl = gl;
        }

        @Override
        public void glLoadIdentity() {
            gl.glLoadIdentity();
        }

        @Override
        public void glMatrixMode(MatrixMode mode) {
            switch (mode) {
            case GL_MODELVIEW:
                gl.glMatrixMode(GL2.GL_MODELVIEW);
                break;
            case GL_PROJECTION:
                gl.glMatrixMode(GL2.GL_PROJECTION);
                break;
            default:
                break;
            }
        }

        @Override
        public GL2GL3 getGL2GL3() {
            return gl;
        }
    }
    
    static class GL3GL2Adapter implements GL2Adapter {
        private GL3 gl;
        private GLMatrixState glMatrixState;
        
        public GL3GL2Adapter(GL3 gl) {
            this.gl = gl;
            this.glMatrixState = new GLMatrixState();
        }

        @Override
        public void glLoadIdentity() {
            glMatrixState.glLoadIdentity();
        }

        @Override
        public void glMatrixMode(MatrixMode mode) {
            switch (mode) {
            case GL_MODELVIEW:
                glMatrixState.glMatrixMode(MatrixMode.GL_MODELVIEW);
                break;
            case GL_PROJECTION:
                glMatrixState.glMatrixMode(MatrixMode.GL_PROJECTION);
                break;
            default:
                break;
            }
        }

        @Override
        public GL2GL3 getGL2GL3() {
            return gl;
        }        
    }
}
