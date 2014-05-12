package org.janelia.it.FlyWorkstation.gui.opengl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.glu.GLU;

import org.janelia.it.FlyWorkstation.geom.Vec3;

/**
 * Creates an adapter with GL2-like methods for either OpenGL versions GL2 or GL3
 * @author brunsc
 *
 */
public class GL2AdapterFactory {
    public static GL2Adapter createGL2Adapter(GLAutoDrawable glDrawable) {
        GL gl = glDrawable.getGL();
        if (! gl.isGL2())
            return new GL3GL2Adapter(gl.getGL3());
        if (gl.isGL2())
            return new GL2GL2Adapter(gl.getGL2());
        return null;
    }
    
    static class GL2GL2Adapter implements GL2Adapter {
        private GL2 gl;
        private static final GLU glu = new GLU();
        
        private float projectionMatrixCache[] = new float[16];
        private float modelViewMatrixCache[] = new float[16];
        
        public GL2GL2Adapter(GL2 gl) {
            this.gl = gl;
        }

        @Override
        public GL2GL3 getGL2GL3() {
            return gl;
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
		public void gluLookAt(double eyeX, double eyeY, double eyeZ,
				double centerX, double centerY, double centerZ, double upX,
				double upY, double upZ) {
			glu.gluLookAt(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);			
		}

		@Override
		public void glFrustum(double left, double right, double bottom,
				double top, double zNear, double zFar) {
			gl.glFrustum(left, right, bottom, top, zNear, zFar);
		}

        @Override
        public float[] getProjectionMatrix() {
            gl.glGetFloatv(GL2.GL_PROJECTION_MATRIX, projectionMatrixCache, 0);
            return projectionMatrixCache;
        }

        @Override
        public float[] getModelViewMatrix() {
            gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, modelViewMatrixCache, 0);
            return modelViewMatrixCache;
        }

        @Override
        public void glTranslated(double x, double y, double z) {
            gl.glTranslated(x, y, z);
        }

		@Override
		public void glPushMatrix() {
			gl.glPushMatrix();
		}

		@Override
		public void glPopMatrix() {
			gl.glPopMatrix();
		}

		@Override
		public void glOrtho(double left, double right, 
				double bottom, double top,
				double nearVal, double farVal) {
			gl.glOrtho(left, right, bottom, top, nearVal, farVal);
		}
    }
    
    static class GL3GL2Adapter implements GL2Adapter {
        private GL3 gl;
        private GLMatrixState glMatrixState = new GLMatrixState();
        private GLLightingState glLightingState = new GLLightingState();
        private GLMaterialState glMaterialState = new GLMaterialState();

        public GL3GL2Adapter(GL3 gl) {
            this.gl = gl;
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

		@Override
		public void gluLookAt(double eyeX, double eyeY, double eyeZ,
				double centerX, double centerY, double centerZ, double upX,
				double upY, double upZ) 
		{
			glMatrixState.getCurrentMatrix().gluLookAt(
					new Vec3(eyeX, eyeY, eyeZ),
					new Vec3(centerX, centerY, centerZ),
					new Vec3(upX, upY, upZ));
		}

		@Override
		public void glFrustum(double left, double right, double bottom,
				double top, double zNear, double zFar) 
		{
			glMatrixState.getCurrentMatrix().glFrustum(
					left, right, bottom, top, zNear, zFar);
		}

        @Override
        public float[] getProjectionMatrix() {
            return glMatrixState.getProjectionMatrix();
        }

        @Override
        public float[] getModelViewMatrix() {
            return glMatrixState.getModelViewMatrix();
        }

        @Override
        public void glTranslated(double x, double y, double z) {
            glMatrixState.getCurrentMatrix().glTranslatef((float)x, (float)y, (float)z);
        }

		@Override
		public void glPushMatrix() {
			glMatrixState.glPushMatrix();
		}

		@Override
		public void glPopMatrix() {
			glMatrixState.glPopMatrix();
		}

		@Override
		public void glOrtho(double left, double right, 
				double bottom, double top,
				double nearVal, double farVal) 
		{
			glMatrixState.getCurrentMatrix().glOrtho(left, right, bottom, top, nearVal, farVal);
		}
    }
}
