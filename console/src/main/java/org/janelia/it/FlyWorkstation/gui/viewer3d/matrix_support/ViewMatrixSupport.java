package org.janelia.it.FlyWorkstation.gui.viewer3d.matrix_support;

import org.janelia.it.FlyWorkstation.geom.Vec3;

/**
 * Adding support for matrix manipulation, broadly-used matrices for OpenGL, etc.
 *
 * Created by fosterl on 3/6/14.
 */
public class ViewMatrixSupport {

    /**
     * Establish a frustum with the corner coords implied. Using this requires knowing the outer parameters
     * of the container first.
     *
     * @param left
     * @param right
     * @param bottom
     * @param top
     * @param near
     * @param far
     * @return
     */
    public float[] frustum( float left, float right, float bottom, float top, float near, float far ) {
        float width = 1.0f / (right - left);
        float height = 1.0f / (top - bottom);
        float depth = 1.0f / (near - far);
        float x = 2.0f * ( near * width);
        float y = 2.0f * ( near * height );
        float A = 2.0f * ((right + left) * width);
        float B = (top + bottom) * height;
        float C = (far + near) * depth;
        float D = 2.0f * (far * near * depth);

        float[] m = new float[ 16 ];
        m[ 0 ] = x;
        m[ 5 ] = y;
        m[ 8 ] = A;
        m[ 9 ] = B;
        m[ 10 ] = C;
        m[ 11 ] = -1.0f;
        m[ 14 ] = D;

        return m;

    }

    /**
     * Creates a perspective matrix. This is used for projection purposes. See
     * http://stackoverflow.com/questions/18404890/how-to-build-perspective-projection-matrix-no-api
     *
     * @param fieldOfViewYDegrees degrees in Y direction, for view
     * @param aspectRatio ratio width v height
     * @param zNear scope of depth: how close to the eye
     * @param zFar scope of depth: how far from the eye
     * @return a perspective matrix to satisfy these parameters.
     */
    public float[] getPerspectiveMatrix( double fieldOfViewYDegrees, double aspectRatio, double zNear, double zFar ) {
        float[] perspectiveMatrix = new float[ 16 ];
        double zDepth = zNear - zFar;                   // This produces a negative number!

        double fieldOfViewYRad = radFromDegree( fieldOfViewYDegrees );

        // This is near / top.
        // Negative 1/tan(1/2 fov) -> right handed.  Positive -> left handed.
        float uh = (float) (1.0 / (Math.tan( 0.5f * fieldOfViewYRad )));
        float oneOverDepth = (float)(1.0 / zDepth);     // ...negative number.

        perspectiveMatrix[ 0 ] = (float)( uh / aspectRatio );  // near / right.
        perspectiveMatrix[ 5 ] =  uh;                           // near / top.
        perspectiveMatrix[ 10 ] = (float)zFar * oneOverDepth;
        perspectiveMatrix[ 11 ] = -1.0f;
        perspectiveMatrix[ 14 ] = (float)((zFar * zNear) * oneOverDepth);  // This produces a negative number!
        return perspectiveMatrix;
    }

    /**
     * Creates a perspective matrix. This is used for projection purposes.
     *
     * @param fieldOfViewY degrees in Y direction, for view
     * @param aspectRatio ratio width v height
     * @param zNear scope of depth: how close to the eye
     * @param zFar scope of depth: how far from the eye
     * @return a perspective matrix to satisfy these parameters.
     */
//    public float[] getPerspectiveMatrix( double fieldOfViewY, double aspectRatio, double zNear, double zFar ) {
//        float[] perspectiveMatrix = new float[ 16 ];
//        double f = Math.atan( fieldOfViewY / 2.0 );
//        double zDepth = zNear - zFar;
//
//        perspectiveMatrix[ 0 ] = (float)(f / aspectRatio);
//        perspectiveMatrix[ 5 ] = (float)f;
//        perspectiveMatrix[ 10 ] = -1.0f;
//        perspectiveMatrix[ 11 ] = (float)((2.0 * zFar * zNear) / zDepth);
//        perspectiveMatrix[ 14 ] = (float)((zNear + zFar) / zDepth);
//        return perspectiveMatrix;
//    }

    public float[] getIdentityMatrix() {
        float[] identity = new float[] {
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f,
        };
        return identity;
    }

    /**
     * Works like "gluLookAt". Will assume 'previous matrix' is identity.
     *
     * @param c center of the scene
     * @param f focus point.
     * @param u Up-in-Ground
     * @return matrix suitable for the viewing transformation.
     */
    public float[] getLookAt(Vec3 eye, Vec3 center, Vec3 up) {
        // Temporary values: forcing vectors to one place for sake of testing.
//        eye = new Vec3( 2, 0, 0 );
//        center = new Vec3( 0, 0, 0 );
//        up = new Vec3( 0, 1, 0 );

        //gl.gluLookAt(c.x(), c.y(), c.z(), // camera in ground
        //        f.x(), f.y(), f.z(), // focus in ground
        //        u.x(), u.y(), u.z()); // up vector in ground

        double[] centerMinusEye = new double[] {
                center.x() - eye.x(),
                center.y() - eye.y(),
                center.z() - eye.z()
        };

        double magCmE = magnitude(centerMinusEye);
        double[] f = new double[] {
                centerMinusEye[0] / magCmE,
                centerMinusEye[1] / magCmE,
                centerMinusEye[2] / magCmE
        };

        double magUp = magnitude(up);
        double[] normalizedUp = new double[] {
                up.getX() / magUp,
                up.getY() / magUp,
                up.getZ() / magUp
        };

        double[] s = cross( f, normalizedUp, 3 );
        double magS = magnitude( s );
        double[] normalizedS = new double[] {
                s[ 0 ] / magS,
                s[ 1 ] / magS,
                s[ 2 ] / magS
        };
        double[] u = cross( normalizedS, f, 3 );
        float[] viewingTransform = new float[] {
                (float)s[ 0 ], (float)u[ 0 ], (float)-f[ 0 ], 0.0f,
                (float)s[ 1 ], (float)u[ 1 ], (float)-f[ 1 ], 0.0f,
                (float)s[ 2 ], (float)u[ 2 ], (float)-f[ 2 ], 0.0f,
                 0.0f,          0.0f,          0.0f,          1.0f,
// Transpose of matrix above.
//
//                (float)s[ 0 ], (float)s[ 1 ], (float)s[ 2 ], 0.0f,
//                (float)u[ 0 ], (float)u[ 1 ], (float)u[ 2 ], 0.0f,
//                (float)-f[ 0 ],(float)-f[ 1 ],(float)-f[ 2 ],0.0f,
//                0.0f,          0.0f,          0.0f,          1.0f,
        };

        // And next need to translate backward from eye coordinates.
        translateM( viewingTransform, 0, (float)-eye.getX(), (float)-eye.getY(), (float)-eye.getZ() );
        return viewingTransform;
    }

    /**
     * Debugging apparatus.
     * @param modelView for MV
     * @param perspective for P
     */
    public void dumpMatrices( float[] modelView, float[] perspective ) {
        dumpMatrix( modelView, "Model-View" );
        dumpMatrix( perspective, "Perspective" );
    }

    private void translateM(
        float[] m, int mOffset,
        float x, float y, float z) {
        for (int i=0 ; i<4 ; i++) {
            int mi = mOffset + i;
            m[12 + mi] += m[mi] * x + m[4 + mi] * y + m[8 + mi] * z;
        }
    }

    private void dumpMatrix( float[] matrix, String label ) {
        System.out.println("=======================" + label + "======================");
        for ( int i = 0; i < 4; i++ ) {
            System.out.print("[");
            for ( int j = 0; j < 4; j++ ) {
                System.out.print( matrix[ i * 4 + j ]);
                System.out.print("\t");
            }
            System.out.print("]");
            System.out.println();
        }

    }
    private double[] cross( double[] v, double[] w, int size ) {
        if ( size == 3  &&  v.length == 3  &&  w.length == 3 ) {
            return new double[] {
                v[1] * w[2] - v[2] * w[1],
                v[2] * w[0] - v[0] * w[2],
                v[0] * w[1] - v[1] * w[0]
            };
        }
        else {
            throw new IllegalArgumentException( "Require a 3x3 matrix, only." );
        }
    }

    private double magnitude(double[] vector) {
        return Math.sqrt(
                vector[0] * vector[0] +
                vector[1] * vector[1] +
                vector[2] * vector[2]
        );
    }

    private double magnitude(Vec3 vector) {
        return Math.sqrt(
                vector.getX() * vector.getX() +
                vector.getY() * vector.getY() +
                vector.getZ() * vector.getZ()
        );
    }

    private double radFromDegree( double degree ) {
        return Math.PI * degree / 360.0;
    }
}
