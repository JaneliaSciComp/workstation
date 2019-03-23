/* 
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.geometry3d;

import java.util.Arrays;
import Jama.Matrix;

/**
 * A 4x4 matrix.
 * These matrices are PRINTED as if they were ordinary matrices,
 * with the translation entries on the right.
 * But for conformity with OpenGL, they are STORED with the 
 * translations entries in positions 13,14,15.
 * 
 * @author brunsc
 */
public class Matrix4 {
    private final float[] data;

    public Matrix4() {
        data = new float[] {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1};
    }
    
    public Matrix4(Matrix4 rhs) {
        data = rhs.data.clone();
    }

    public Matrix4(float[] rhs) {
        data = rhs.clone();
    }
    
    public Matrix4(Matrix rhs) {
            double[] d = rhs.getColumnPackedCopy(); // TODO - which row/column packed array?
            assert(d.length == 16);
            data = new float[d.length];
            for (int i = 0; i < d.length; ++i)
                data[i] = (float)d[i];
    }

    public Matrix4(float e00,
            float e01,
            float e02,
            float e03,
            float e10,
            float e11,
            float e12,
            float e13,
            float e20,
            float e21,
            float e22,
            float e23,
            float e30,
            float e31,
            float e32,
            float e33) 
    {
        data = new float[] {
            e00, e01, e02, e03,
            e10, e11, e12, e13,
            e20, e21, e22, e23,
            e30, e31, e32, e33};
    }
    
    public float[] asArray() {
        return data;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Arrays.hashCode(this.data);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Matrix4 other = (Matrix4) obj;
        if (!Arrays.equals(this.data, other.data)) {
            return false;
        }
        return true;
    }
    
    public Matrix4 makePerspective(float fovYRadians, float aspect, float zNear, float zFar) {
        // https://developer.apple.com/library/mac/documentation/Darwin/Reference/ManPages/man3/gluPerspective.3.html
        float f = 1 / (float)Math.tan(0.5 * fovYRadians);
        setTranspose(
                f/aspect, 0, 0, 0,
                0, f, 0, 0,
                0, 0, (zFar+zNear)/(zNear-zFar), (2*zFar*zNear)/(zNear-zFar),
                0, 0, -1, 0
        );
        return this;
    }
    
    public Matrix4 makeFrustum(float left, float right, float bottom, float top,
			float zNear, float zFar) 
    {
        // http://www.opengl.org/sdk/docs/man2/xhtml/glFrustum.xml
        float A = (right + left) / (right - left);
        float B = (top + bottom) / (top - bottom);
        float C = -(zFar + zNear) / (zFar - zNear);
        float D = -2*zFar*zNear/(zFar - zNear);
        set(2*zNear/(right-left), 0, 0, 0,
            0, 2*zNear/(top-bottom), 0, 0,
            A, B, C, -1,
            0, 0, D, 0);
        return this;
    }
    
    /**
     * http://www.opengl.org/sdk/docs/man2/xhtml/glOrtho.xml
     * @param left
     * @param right
     * @param bottom
     * @param top
     * @param near
     * @param far
     * @return 
     */
    Matrix4 makeOrthographic(float left, float right, float bottom, float top, float near, float far) {
        float tx = -(right + left)/(right - left);
        float ty = -(top + bottom)/(top - bottom);
        float tz = -(far + near)/(far - near);
        setTranspose(
                2/(right - left), 0, 0, tx,
                0, 2/(top - bottom), 0, ty,
                0, 0, -2/(far - near), tz,
                0, 0, 0, 1);
        return this;
    }
    
    public void set(
            float e00,
            float e01,
            float e02,
            float e03,
            float e10,
            float e11,
            float e12,
            float e13,
            float e20,
            float e21,
            float e22,
            float e23,
            float e30,
            float e31,
            float e32,
            float e33) 
    {
        data[0] = e00;
        data[1] = e01;
        data[2] = e02;
        data[3] = e03;
        data[4] = e10;
        data[5] = e11;
        data[6] = e12;
        data[7] = e13;
        data[8] = e20;
        data[9] = e21;
        data[10] = e22;
        data[11] = e23;
        data[12] = e30;
        data[13] = e31;
        data[14] = e32;
        data[15] = e33;
    }

    
    /**
     * Transpose version, to ease transcription from column-major OpenGL specs
     */
    public Matrix4 setTranspose(
            float e00,
            float e10,
            float e20,
            float e30,
            float e01,
            float e11,
            float e21,
            float e31,
            float e02,
            float e12,
            float e22,
            float e32,
            float e03,
            float e13,
            float e23,
            float e33) 
    {
        data[0] = e00;
        data[1] = e01;
        data[2] = e02;
        data[3] = e03;
        data[4] = e10;
        data[5] = e11;
        data[6] = e12;
        data[7] = e13;
        data[8] = e20;
        data[9] = e21;
        data[10] = e22;
        data[11] = e23;
        data[12] = e30;
        data[13] = e31;
        data[14] = e32;
        data[15] = e33;
        return this;
    }

    /**
     * @return this matrix
     * Reset this matrix to identity
     */
    public final Matrix4 identity() {
        set(1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1);
        return this;
    }
    
    public Matrix4 inverse() {
        return new Matrix4(toJama().inverse());
    }
    
    public Matrix4 multiply(Matrix4 rhs) {
        float[] lhs = data.clone(); // copy before in-place replacement
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                data[4*i+j] = 0;
                for (int k = 0; k < 4; ++k) {
                    data[4*i+j] += lhs[4*i+k] * rhs.data[4*k+j];
                }
            }
        }
        return this;
    }
    
    public Vector4 multiply(Vector4 rhs) {
        Vector4 result = new Vector4(0,0,0,0);
        for (int i = 0; i < 4; ++i) {
            float val = 0;
            for (int j = 0; j < 4; ++j) {
                val += rhs.get(j) * data[4*j+i]; // transpose matrix convention...
            }
            result.set(i, val);
        }
        return result;
    }
    
    public Matrix toJama() {
        double[][] d = new double[4][4];
        for (int i = 0; i < 4; ++i)
            for (int j = 0; j < 4; ++j)
                d[j][i] = data[i*4 + j]; // conventioned transposed between Jama and Matrix4
        return new Matrix(d);
    }
    
    @Override
    public String toString() {
        return "[" 
                + data[0] + ", " + data[4] + ", " + data[8] + ", " + data[12] + "\n"
                + " " + data[1] + ", " + data[5] + ", " + data[9] + ", " + data[13] + "\n"
                + " " + data[2] + ", " + data[6] + ", " + data[10] + ", " + data[14] + "\n"
                + " " + data[3] + ", " + data[7] + ", " + data[11] + ", " + data[15]
                + "]";
    }

    public Matrix4 copy(Matrix4 rhs) {
        System.arraycopy(rhs.data, 0, data, 0, 16);
        return this;
    }

    Matrix4 scale(float sx, float sy, float sz) {
        return multiply(new Matrix4(
                sx, 0, 0, 0,
                0, sy, 0, 0,
                0, 0, sz, 0,
                0, 0, 0, 1)
        );        
    }
    
    Matrix4 translate(Vector3 t) {
        return multiply(new Matrix4(
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                t.getX(), t.getY(), t.getZ(), 1)
        );
    }

    public Matrix4 transpose() {
        set(data[0], data[4], data[8], data[12],
            data[1], data[5], data[9], data[13],
            data[2], data[6], data[10], data[14],
            data[3], data[7], data[11], data[15]);
        return this;
    }

    Matrix4 rotate(Rotation rotation) {
        return multiply(rotation.asTransform());
    }
}
