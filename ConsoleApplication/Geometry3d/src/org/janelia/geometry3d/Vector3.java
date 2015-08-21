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

/**
 *
 * @author brunsc
 */
public class Vector3 extends BasicVector 
implements ConstVector3 
{
    // private final float[] data; // provided by base class

    public Vector3(float x, float y, float z) {
        super(3);
        data[0] = x;
        data[1] = y;
        data[2] = z;
    }

    /**
     * Copy constructor, to avoid broken Java clone() approach.
     * @param cloned 
     */
    public Vector3(Vector3 cloned) {
        super(cloned);
    }
    
    public Vector3 applyRotation(Rotation r) {
        float[] p = data.clone(); // copy before overwrite
        float[] R = r.asArray();
        for (int i = 0; i < 3; ++i) {
            data[i] = 0;
            for (int j = 0; j < 3; ++j) {
                data[i] += R[3*i+j] * p[j];
            }
        }   
        return this;
    }
    
    public Vector3 add(Vector3 rhs) {
        for (int i = 0; i < 3; ++i)
            data[i] += rhs.data[i];
        return this;
    }

    public void copy(Vector3 rhs) {
        System.arraycopy(rhs.data, 0, data, 0, 3);
    }
    
    @Override
    public Vector3 cross(ConstVector3 rhs) {
        float x = getY()*rhs.getZ() - getZ()*rhs.getY();
        float y = getZ()*rhs.getX() - getX()*rhs.getZ();
        float z = getX()*rhs.getY() - getY()*rhs.getX();
        return new Vector3(x, y, z);
    }
    
    @Override
    public float getX() {return data[0];}
    @Override
    public float getY() {return data[1];}
    @Override
    public float getZ() {return data[2];}
    
    /**
     * Inverts this vector.
     * @return this vector
     */
    public Vector3 negate() {
        return multiplyScalar(-1);
    }

    @Override
    public float length() {
        return (float)Math.sqrt(lengthSquared());
    }
    
    public Vector3 normalize() {
        float scale = 1.0f/length();
        return this.multiplyScalar(scale);
    }
    
    @Override
    public float lengthSquared() {
        return this.dot(this);
    }
    
    /**
     * Multiplies this vector by a scalar s
     * @param s
     * @return 
     */
    public Vector3 multiplyScalar(float s) {
        for (int i = 0; i < 3; ++i)
            data[i] *= s;
        return this;    
    }

    /**
     * Creates a Matrix4, representing this Vector3 as a translation.
     * @return 
     */
    @Override
    public Matrix4 asTransform() {
        return new Matrix4(
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                data[0], data[1], data[2], 1);
    }

    public void set(float x, float y, float z) {
        data[0] = x;
        data[1] = y;
        data[2] = z;
    }
    
    public Vector3 setX(float x) {
        data[0] = x;
        return this;
    }
    public Vector3 setY(float y) {
        data[1] = y;
        return this;
    }
    public Vector3 setZ(float z) {
        data[2] = z;
        return this;
    }

    public Vector3 sub(Vector3 rhs) {
        super.sub(rhs);
        return this;
    }
}
