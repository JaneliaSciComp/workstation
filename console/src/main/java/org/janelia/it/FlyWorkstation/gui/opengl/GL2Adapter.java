package org.janelia.it.FlyWorkstation.gui.opengl;

import javax.media.opengl.GL2GL3;

/**
 * Allow some parts of GL2 to be faked up in GL3
 * @author brunsc
 *
 */
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
    
    /**
     * define a viewing transformation
     * 
     * @param eyeX Specifies the position of the eye point.
     * @param eyeY
     * @param eyeZ
     * @param centerX Specifies the position of the reference point.
     * @param centerY
     * @param centerZ
     * @param upX Specifies the direction of the up vector.
     * @param upY
     * @param upZ
     */
    void gluLookAt(
    		double eyeX,  double eyeY,  double eyeZ,  
    		double centerX,  double centerY,  double centerZ,  
    		double upX,  double upY,  double upZ);

    /**
     * multiply the current matrix by a perspective matrix
     * @param left Specify the coordinates for the left and right vertical clipping planes.
     * @param right
     * @param bottom Specify the coordinates for the bottom and top horizontal clipping planes.
     * @param top
     * @param zNear Specify the distances to the near and far depth clipping planes.
                    Both distances must be positive.
     * @param zFar
     */
	void glFrustum(double left, double right, 
			double bottom, double top, 
			double zNear, double zFar);

    float[] getProjectionMatrix();
    float[] getModelViewMatrix();

}
