package org.janelia.it.FlyWorkstation.gui.viewer3d.matrix_support;

import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adding support for matrix manipulation, broadly-used matrices for OpenGL, etc.
 *
 * Created by fosterl on 3/6/14.
 */
public class ViewMatrixSupport {

    private Logger logger = LoggerFactory.getLogger(ViewMatrixSupport.class);

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
     * @deprecated until actually tested.
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
     * See also,
     * android.opengl.Matrix which employs zNear - zFar, and the other negative values used herein.
     *
     * @param fieldOfViewYDegrees degrees in Y direction, for view
     * @param aspectRatio ratio width v height
     * @param zNear scope of depth: how close to the eye
     * @param zFar scope of depth: how far from the eye
     * @return a perspective matrix to satisfy these parameters.
     */
    public float[] getPerspectiveMatrix_ALT( double fieldOfViewYDegrees, double aspectRatio, double zNear, double zFar ) {
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
     * See also,
     * android.opengl.Matrix
     *
     * @param fieldOfViewYDegrees degrees in Y direction, for view
     * @param aspectRatio ratio width v height
     * @param zNear scope of depth: how close to the eye
     * @param zFar scope of depth: how far from the eye
     * @return a perspective matrix to satisfy these parameters.
     */
    public float[] getPerspectiveMatrix( double fieldOfViewYDegrees, double aspectRatio, double zNear, double zFar ) {
        float[] perspectiveMatrix = new float[ 16 ];
        float rangeReciprocal = 1.0f / (float)(zNear - zFar);
        float f = 1.0f / (float) Math.tan(fieldOfViewYDegrees * (Math.PI / 360.0));

        perspectiveMatrix[ 0 ] = (float)( f / aspectRatio );  // near / right.
        perspectiveMatrix[ 5 ] =  f;                           // near / top.
        perspectiveMatrix[ 10 ] = (float)(zNear + zFar) * rangeReciprocal;
        perspectiveMatrix[ 11 ] = -1.0f;
        perspectiveMatrix[ 14 ] = 2.0f * (float)((zFar * zNear) * rangeReciprocal);  // This produces a negative number!

        // This shows: m[10] = 1/2 alt m[10], and m[14] = 3/2 alt m[14]
        //float[] altPerspectiveMatrix = getPerspectiveMatrix_ALT( fieldOfViewYDegrees, aspectRatio, zNear, zFar);
        //for ( int i = 0; i < perspectiveMatrix.length; i++ ) {
        //    if ( Math.abs( perspectiveMatrix[i] - altPerspectiveMatrix[i] ) > 0.001 ) {
        //        System.out.println("Got Diff in Matrices at " + i + " of " + perspectiveMatrix[i] + " vs " + altPerspectiveMatrix[i]);
        //    }
        //}
        return perspectiveMatrix;
    }

    /** Because the world matrix can change, it is necessary to use it to affect normals. */
    public float[] computeNormalMatrix( float[] modelViewMatrix ) {
        float[] rtnVal = new float[ 16 ];
        // Want to use only the top/left 3x3 of the model-view matrix.
        //  Pg 402 of "Interactive Computer Graphics A Top-Down Shader Based Approach with OpenGL"
        float[] normalPart = new float[16];
        System.arraycopy( modelViewMatrix, 0, normalPart, 0, 16 );
        normalPart[ 3 ] = 0.0f;
        normalPart[ 7 ] = 0.0f;
        normalPart[ 11 ] = 0.0f;
        normalPart[ 12 ] = 0.0f;
        normalPart[ 13 ] = 0.0f;
        normalPart[ 14 ] = 0.0f;
        normalPart[ 15 ] = 1.0f;

        float[] temp2 = new float[ 16 ];
        boolean couldInvert = invertM(temp2, 0, normalPart, 0);
        if (! couldInvert ) {
            logger.error("ModelPos-Inv", "Could not invert matrix");
            System.arraycopy( rtnVal, 0, modelViewMatrix, 0, 16 );
        }
        else {
            transposeM(rtnVal, 0, temp2, 0);
        }

        return rtnVal;
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
    public float[] getLookAt(Vec3 eye, Vec3 center, Vec3 up) {
        // This is the 'prototype' from the old fixed-function pipeline arena.
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
        };

        // And next need to translate backward from eye coordinates.
        translateM( viewingTransform, 0, (float)-eye.getX(), (float)-eye.getY(), (float)-eye.getZ() );
        return viewingTransform;
    }

    /**
     * Debugging apparatus. Takes the two matrices as starting point.  Calculates the normal matrix and
     * dumps that too.
     * @param modelView for MV
     * @param perspective for P
     */
    public void dumpMatrices( float[] modelView, float[] perspective ) {
        dumpMatrix( modelView, "Model-View" );
        dumpMatrix( perspective, "Perspective" );
        float[] normalMatrix = computeNormalMatrix( modelView );
        dumpMatrix( normalMatrix, "Normals" );
    }

    /**
     * This has been borrowed directly from
     * Copyright (C) 2007 The Android Open Source Project
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *      http://www.apache.org/licenses/LICENSE-2.0
     */
    public static void  transposeM(float[] mTrans, int mTransOffset, float[] m,
                                   int mOffset) {
        for (int i = 0; i < 4; i++) {
            int mBase = i * 4 + mOffset;
            mTrans[i + mTransOffset] = m[mBase];
            mTrans[i + 4 + mTransOffset] = m[mBase + 1];
            mTrans[i + 8 + mTransOffset] = m[mBase + 2];
            mTrans[i + 12 + mTransOffset] = m[mBase + 3];
        }
    }

    public static boolean invertM(float[] mInv, int mInvOffset, float[] m,
                                  int mOffset) {
        // Invert a 4 x 4 matrix using Cramer's Rule
        // transpose matrix
        final float src0  = m[mOffset +  0];
        final float src4  = m[mOffset +  1];
        final float src8  = m[mOffset +  2];
        final float src12 = m[mOffset +  3];
        final float src1  = m[mOffset +  4];
        final float src5  = m[mOffset +  5];
        final float src9  = m[mOffset +  6];
        final float src13 = m[mOffset +  7];
        final float src2  = m[mOffset +  8];
        final float src6  = m[mOffset +  9];
        final float src10 = m[mOffset + 10];
        final float src14 = m[mOffset + 11];
        final float src3  = m[mOffset + 12];
        final float src7  = m[mOffset + 13];
        final float src11 = m[mOffset + 14];
        final float src15 = m[mOffset + 15];

        // calculate pairs for first 8 elements (cofactors)
        final float atmp0  = src10 * src15;
        final float atmp1  = src11 * src14;
        final float atmp2  = src9  * src15;
        final float atmp3  = src11 * src13;
        final float atmp4  = src9  * src14;
        final float atmp5  = src10 * src13;
        final float atmp6  = src8  * src15;
        final float atmp7  = src11 * src12;
        final float atmp8  = src8  * src14;
        final float atmp9  = src10 * src12;
        final float atmp10 = src8  * src13;
        final float atmp11 = src9  * src12;

        // calculate first 8 elements (cofactors)
        final float dst0  = (atmp0 * src5 + atmp3 * src6 + atmp4  * src7)
                - (atmp1 * src5 + atmp2 * src6 + atmp5  * src7);
        final float dst1  = (atmp1 * src4 + atmp6 * src6 + atmp9  * src7)
                - (atmp0 * src4 + atmp7 * src6 + atmp8  * src7);
        final float dst2  = (atmp2 * src4 + atmp7 * src5 + atmp10 * src7)
                - (atmp3 * src4 + atmp6 * src5 + atmp11 * src7);
        final float dst3  = (atmp5 * src4 + atmp8 * src5 + atmp11 * src6)
                - (atmp4 * src4 + atmp9 * src5 + atmp10 * src6);
        final float dst4  = (atmp1 * src1 + atmp2 * src2 + atmp5  * src3)
                - (atmp0 * src1 + atmp3 * src2 + atmp4  * src3);
        final float dst5  = (atmp0 * src0 + atmp7 * src2 + atmp8  * src3)
                - (atmp1 * src0 + atmp6 * src2 + atmp9  * src3);
        final float dst6  = (atmp3 * src0 + atmp6 * src1 + atmp11 * src3)
                - (atmp2 * src0 + atmp7 * src1 + atmp10 * src3);
        final float dst7  = (atmp4 * src0 + atmp9 * src1 + atmp10 * src2)
                - (atmp5 * src0 + atmp8 * src1 + atmp11 * src2);

        // calculate pairs for second 8 elements (cofactors)
        final float btmp0  = src2 * src7;
        final float btmp1  = src3 * src6;
        final float btmp2  = src1 * src7;
        final float btmp3  = src3 * src5;
        final float btmp4  = src1 * src6;
        final float btmp5  = src2 * src5;
        final float btmp6  = src0 * src7;
        final float btmp7  = src3 * src4;
        final float btmp8  = src0 * src6;
        final float btmp9  = src2 * src4;
        final float btmp10 = src0 * src5;
        final float btmp11 = src1 * src4;
        // calculate second 8 elements (cofactors)
        final float dst8  = (btmp0  * src13 + btmp3  * src14 + btmp4  * src15)
                - (btmp1  * src13 + btmp2  * src14 + btmp5  * src15);
        final float dst9  = (btmp1  * src12 + btmp6  * src14 + btmp9  * src15)
                - (btmp0  * src12 + btmp7  * src14 + btmp8  * src15);
        final float dst10 = (btmp2  * src12 + btmp7  * src13 + btmp10 * src15)
                - (btmp3  * src12 + btmp6  * src13 + btmp11 * src15);
        final float dst11 = (btmp5  * src12 + btmp8  * src13 + btmp11 * src14)
                - (btmp4  * src12 + btmp9  * src13 + btmp10 * src14);
        final float dst12 = (btmp2  * src10 + btmp5  * src11 + btmp1  * src9 )
                - (btmp4  * src11 + btmp0  * src9  + btmp3  * src10);
        final float dst13 = (btmp8  * src11 + btmp0  * src8  + btmp7  * src10)
                - (btmp6  * src10 + btmp9  * src11 + btmp1  * src8 );
        final float dst14 = (btmp6  * src9  + btmp11 * src11 + btmp3  * src8 )
                - (btmp10 * src11 + btmp2  * src8  + btmp7  * src9 );
        final float dst15 = (btmp10 * src10 + btmp4  * src8  + btmp9  * src9 )
                - (btmp8  * src9  + btmp11 * src10 + btmp5  * src8 );
        // calculate determinant
        final float det =
                src0 * dst0 + src1 * dst1 + src2 * dst2 + src3 * dst3;
        if (det == 0.0f) {
            return false;
        }

        // calculate matrix inverse
        final float invdet = 1.0f / det;
        mInv[     mInvOffset] = dst0  * invdet;
        mInv[ 1 + mInvOffset] = dst1  * invdet;
        mInv[ 2 + mInvOffset] = dst2  * invdet;
        mInv[ 3 + mInvOffset] = dst3  * invdet;
        mInv[ 4 + mInvOffset] = dst4  * invdet;
        mInv[ 5 + mInvOffset] = dst5  * invdet;
        mInv[ 6 + mInvOffset] = dst6  * invdet;
        mInv[ 7 + mInvOffset] = dst7  * invdet;
        mInv[ 8 + mInvOffset] = dst8  * invdet;
        mInv[ 9 + mInvOffset] = dst9  * invdet;
        mInv[10 + mInvOffset] = dst10 * invdet;
        mInv[11 + mInvOffset] = dst11 * invdet;
        mInv[12 + mInvOffset] = dst12 * invdet;
        mInv[13 + mInvOffset] = dst13 * invdet;
        mInv[14 + mInvOffset] = dst14 * invdet;
        mInv[15 + mInvOffset] = dst15 * invdet;
        return true;
    }

    /**
     * "Move" the effect of some matrix, by x, y, and | or z components.
     *
     * @param m matrix to modify.
     * @param mOffset into the matrix
     * @param x distance of X
     * @param y of Y
     * @param z of Z
     */
    public void translateM(
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
        return Math.PI * degree / 180.0;
    }
}
