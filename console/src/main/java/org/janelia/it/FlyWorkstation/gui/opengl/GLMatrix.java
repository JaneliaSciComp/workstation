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
    private float[] d = {
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
        float[] M = {
            (float)s.getX(), (float)u.getX(), (float)-f.getX(), 0,
            (float)s.getY(), (float)u.getY(), (float)-f.getY(), 0,
            (float)s.getZ(), (float)u.getZ(), (float)-f.getZ(), 0,
                   0,         0,        0, 1};
        glMultMatrixf(M);
        glTranslatef((float)-eye.getX(), (float)-eye.getY(), (float)-eye.getZ());
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
    void glMultMatrixf(float[] m) {
        float result[] = {
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
    
    void glTranslatef(float x, float y, float z) {
        glMultMatrixf(new float[] {
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                x, y, z, 1});
    }
    
    /**
     * Helper to convert row major to column major, to match visuals of OpenGL docs
     */
    protected static float[] transpose(float m[]) {
        return new float[] {
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
		double C = -(zFar + zNear) / (zFar - zNear);
		double D = -2*zFar*zNear/(zFar - zNear);
        glMultMatrixf(new float[] {
                (float)(2*zNear/(right-left)), 0, 0, 0,
                0, (float)(2*zNear/(top-bottom)), 0, 0,
                (float)A, (float)B, (float)C, -1,
                0, 0, (float)D, 0});
	}

    public float[] getFloatArray() {
        return d;
    }

	public GLMatrix cloneMatrix() {
		GLMatrix result = new GLMatrix();
		for (int i = 0; i < 16; ++i)
			result.d[i] = d[i];
		return result;
	}

	public void glOrtho(double left, double right, double bottom, double top,
			double nearVal, double farVal) 
	{
		// http://www.opengl.org/sdk/docs/man2/xhtml/glOrtho.xml
		float tx = -(float)((right+left)/(right-left));
		float ty = -(float)((top+bottom)/(top-bottom));
		float tz = -(float)((farVal+nearVal)/(farVal-nearVal));
		glMultMatrixf(new float[] {
			2/(float)(right-left), 0, 0, 0,
			0, 2/(float)(top-bottom), 0, 0,
			0, 0, -2/(float)(farVal-nearVal), 0,
			tx, ty, tz, 1,
		});
	}

}
