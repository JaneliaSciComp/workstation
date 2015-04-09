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

/**
 *
 * @author Christopher Bruns
 */
public class BasicVector implements ConstVector 
{
    protected final float[] data;
    
    public BasicVector(int size) {
        data = new float[size];
    }

    /**
     * Copy constructor, to avoid broken Java clone() approach.
     * @param cloned 
     */
    public BasicVector(BasicVector cloned) {
        data = cloned.data.clone();
    }
    
    public float[] toArray() {
        return data;
    }

    @Override
    public float[] toNewArray() {
        return Arrays.copyOf(data, data.length);
    }

    // TODO unchecked size
    @Override
    public float dot(BasicVector rhs) {
        float result = 0;
        for (int i = 0; i < data.length; ++i)
            result += data[i]*rhs.data[i];
        return result;
    }
    
    @Override
    public float distance(BasicVector rhs) {
        return (float)Math.sqrt(this.distanceSquared(rhs));
    }

    @Override
    public float distanceSquared(BasicVector rhs) {
        BasicVector v = new BasicVector(this).sub(rhs);
        return v.dot(v);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BasicVector other = (BasicVector) obj;
        return Arrays.equals(this.data, other.data);
    }

    @Override
    public float get(int i) {
        return data[i];
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.data);
    }

    public void set(int ix, float v) {
        data[ix] = v;
    }
    
    @Override
    public int size() {
        return data.length;
    }

    public BasicVector sub(BasicVector rhs) {
        for (int i = 0; i < data.length; ++i)
            data[i] -= rhs.data[i];
        return this;
    }
    
    @Override
    public String toString() {
        return Arrays.toString(data);
    }

}
