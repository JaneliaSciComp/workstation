package org.janelia.it.FlyWorkstation.gui.opengl;

import org.janelia.it.FlyWorkstation.geom.Vec3;

/**
 * Matrix implementation to help recapitulate the fixed functionality
 * lost after OpenGL version 3.1.
 * 
 * @author brunsc
 *
 */
public class GLMatrix {

    /**
     * Store in column major order, to match OpenGL docs    
     */
    private double[] d = {
        1, 0, 0, 0, // looks like a row, but it's the first COLUMN
        0, 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 1,
    };
    
    /**
     * @param eye Specifies the position of the eye point.
     * @param center Specifies the position of the reference point.
     * @param up Specifies the direction of the up vector.
     */
    public void gluLookAt(
            Vec3 eye,
            Vec3 center,
            Vec3 up)
    {
        // http://www.opengl.org/sdk/docs/man2/xhtml/gluLookAt.xml
        Vec3 f = center.minus(eye);
        f = f.times(1.0 / f.norm());
        Vec3 upNorm = up.times(1.0 / up.norm());
        Vec3 s = f.cross(upNorm);
        Vec3 sNorm = s.times(1.0/s.norm());
        Vec3 u = sNorm.cross(f);
        // Transpose to keep with opengl docs visual nomenclature
        double[] M = {
            s.getX(), u.getX(), -f.getX(), 0,
            s.getY(), u.getY(), -f.getY(), 0,
            s.getZ(), u.getZ(), -f.getZ(), 0,
                   0,         0,        0, 1};
        glMultMatrixd(M);
        glTranslated(-eye.getX(), -eye.getY(), -eye.getZ());
    }
    
    /**
     * Replace the current matrix with the identity matrix
     */
    void glLoadIdentity() {
        d[0] = 1;
        d[1] = 0;
        d[2] = 0;
        d[3] = 0;
        
        d[4] = 0;
        d[5] = 1;
        d[6] = 0;
        d[7] = 0;
        
        d[8] = 0;
        d[9] = 0;
        d[10] = 1;
        d[11] = 0;
        
        d[12] = 0;
        d[13] = 0;
        d[14] = 0;
        d[15] = 1;
    }

    /**
     * Multiply this matrix with the specified matrix
     * @param m Points to 16 consecutive values that are used as the elements 
     * of a 4 × 4 column-major matrix.   
     */
    void glMultMatrixd(double[] m) {
        double result[] = {
                0,0,0,0,
                0,0,0,0,
                0,0,0,0,
                0,0,0,0};
        for (int i = 0; i < 4; ++i)
            for (int j = 0; j < 4; ++j)
                for (int k = 0; k < 4; ++k)
                    result[i+j*4] += d[i+k*4]*m[k+j*4]; // TODO test
        d = result;
    }
    
    void glTranslated(double x, double y, double z) {
        glMultMatrixd(new double[] {
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                x, y, z, 1});
    }
    
    /**
     * Helper to convert row major to column major, to match visuals of OpenGL docs
     */
    protected static double[] transpose(double m[]) {
        return new double[] {
                m[0], m[4], m[8], m[12],
                m[1], m[5], m[9], m[13],
                m[2], m[6], m[10], m[14],
                m[3], m[7], m[11], m[15]};
    }

	public void glFrustum(double left, double right, double bottom, double top,
			double zNear, double zFar) 
	{
		// http://www.opengl.org/sdk/docs/man2/xhtml/glFrustum.xml
		double A = (right + left) / (right - left);
		double B = (top + bottom) / (top - bottom);
		double C = (zFar + zNear) / (zFar - zNear);
		double D = 2*zFar*zNear/(zFar - zNear);
        glMultMatrixd(new double[] {
                2*zNear/(right-left), 0, 0, 0,
                0, 2*zNear/(top-bottom), 0, 0,
                A, B, C, -1,
                0, 0, D, 0});
	}

}
