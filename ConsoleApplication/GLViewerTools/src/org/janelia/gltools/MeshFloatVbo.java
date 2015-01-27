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
package org.janelia.gltools;

import com.jogamp.common.nio.Buffers;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.GL3;

/**
 * Utility class intended for use by MeshActor internals.
 * Only supports float type vertex attributes at the moment.
 * @author brunsc
 */
public class MeshFloatVbo implements GL3Resource 
{
    private int bufferByteStride = 0;
    private int vertexCount = 0;
    private int vboHandle = 0;
    private VertexAttribute[] attributes;
    //
    private List<Float> floatStorage = new ArrayList<Float>();
    private boolean verticesNeedUpload = true;
    
    /**
     * 
     * 
     */
    public MeshFloatVbo(VertexAttribute[] attributes) {
        initialize(attributes);
    }

    MeshFloatVbo(List<VertexAttribute> attributes) {
        VertexAttribute[] a = {};
        a = attributes.toArray(a);
        initialize(a);
    }
    
    public void clear() {
        if (floatStorage.size() < 1)
            return; // already clear
        vertexCount = 0;
        floatStorage.clear();
        verticesNeedUpload = true;
    }
    
    private void initialize(VertexAttribute[] attributes) {
        this.attributes = attributes;
        bufferByteStride = 0; // bytes
        for (VertexAttribute att : attributes) {
            att.setBufferByteOffset(bufferByteStride);
            bufferByteStride += att.getAttributeByteStride();
        }        
    }
    
    public void append(float value) {
        floatStorage.add(value);
        // System.out.println("vbo " + this + ", float " + value);
        verticesNeedUpload = true;
    }
    
    public void append(float[] values) {
        for (float f : values)
            append(f);
    }
    
    @Override
    public void dispose(GL3 gl) {
        if (floatStorage.size() > 0)
            verticesNeedUpload = true;
        if (vboHandle == 0)
            return;
        int [] vbos = {vboHandle};
        gl.glDeleteBuffers(1, vbos, 0);
        vboHandle = 0;
    }

    @Override
    public void init(GL3 gl) {
        if (vboHandle != 0)
            return; // already initialized
        int [] vbos = {0};
        gl.glGenBuffers(1, vbos, 0);
        vboHandle = vbos[0];
        if (floatStorage.size() > 0)
            verticesNeedUpload = true;
    }
    
    public void bind(GL3 gl, int shaderHandle) {
        if (vboHandle == 0)
            return;
        if (shaderHandle == 0)
            return;
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vboHandle);
        if (verticesNeedUpload) {
            FloatBuffer floatBuffer = Buffers.newDirectFloatBuffer(
                    floatStorage.size());
            for (float f : floatStorage)
                floatBuffer.put(f);
            floatBuffer.flip();
            gl.glBufferData(GL3.GL_ARRAY_BUFFER,
                floatStorage.size() * Buffers.SIZEOF_FLOAT,
                floatBuffer,
                GL3.GL_STATIC_DRAW);
            verticesNeedUpload = false;
        }
        for (VertexAttribute att : attributes)
            att.bind(gl, bufferByteStride, shaderHandle);
    }
    
    public void unbind(GL3 gl) {
        if (vboHandle == 0)
            return;
        for (VertexAttribute att : attributes)
            att.unbind(gl);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);        
    }

    int getVertexCount() {
        return vertexCount;
    }

    public static class VertexAttribute {
        private String attributeName = "position";
        private int componentCount = 3;
        private final boolean normalizeInts = false;
        private final int componentType = GL3.GL_FLOAT;
        private int bufferByteOffset = 0;
        private int locationInShader = -1;

        /**
         * 
         * @param attributeName must match corresponding attribute name in the shader
         * @param componentCount 
         */
        public VertexAttribute(String attributeName, int componentCount) {
            this.attributeName = attributeName;
            this.componentCount = componentCount;
        }
        
        public int getAttributeByteStride() {
            return componentCount * Float.SIZE/8;
        }
        
        public void bind(GL3 gl, int bufferStrideBytes, int shaderHandle) 
        {
            if (shaderHandle == 0)
                return;
            locationInShader = gl.glGetAttribLocation(
                    shaderHandle, attributeName);
            if (locationInShader < 0)
                return; // attribute not used
            gl.glEnableVertexAttribArray(locationInShader);
            gl.glVertexAttribPointer(
                    locationInShader,
                    componentCount,
                    componentType,
                    normalizeInts,
                    bufferStrideBytes,
                    bufferByteOffset);
        }
        
        public void unbind(GL3 gl) {
            if (locationInShader < 0)
                return; // attribute not used
            gl.glDisableVertexAttribArray(locationInShader);
        }

        private void setBufferByteOffset(int offset) {
            bufferByteOffset = offset;
        }
    }

}
