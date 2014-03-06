package org.janelia.it.FlyWorkstation.gui.viewer3d.matrix_support;

import org.janelia.it.FlyWorkstation.geom.Vec3;

/**
 * Adding support for matrix manipulation, broadly-used matrices for OpenGL, etc.
 *
 * Created by fosterl on 3/6/14.
 */
public class ViewMatrixSupport {

    /**
     * Creates a perspective matrix. This is used for projection purposes.
     *
     * @param fieldOfViewY degrees in Y direction, for view
     * @param aspectRatio ratio width v height
     * @param zNear scope of depth: how close to the eye
     * @param zFar scope of depth: how far from the eye
     * @return a perspective matrix to satisfy these parameters.
     */
    public float[] getPerspectiveMatrix( double fieldOfViewY, double aspectRatio, double zNear, double zFar ) {
        float[] perspectiveMatrix = new float[ 16 ];
        double f = Math.atan( fieldOfViewY / 2.0 );
        double zDepth = zNear - zFar;

        perspectiveMatrix[ 0 ] = (float)(f / aspectRatio);
        perspectiveMatrix[ 5 ] = (float)f;
        perspectiveMatrix[ 10 ] = (float)((zNear + zFar) / zDepth);
        perspectiveMatrix[ 11 ] = (float)((2.0 * zFar * zNear) / zDepth);
        perspectiveMatrix[ 14 ] = -1.0f;
        return perspectiveMatrix;
    }

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
    public float[] getViewingTransform(Vec3 eye, Vec3 center, Vec3 up) {
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
                (float)s[ 0 ], (float)s[ 1 ], (float)s[ 2 ], 0.0f,
                (float)u[ 0 ], (float)u[ 1 ], (float)u[ 2 ], 0.0f,
                (float)-f[ 0 ],(float)-f[ 1 ],(float)-f[ 2 ],0.0f,
                0.0f,          0.0f,          0.0f,          1.0f,
        };

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
}
