
package org.janelia.gltools;

import com.jogamp.common.nio.Buffers;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.GL3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final List<Float> floatStorage = new ArrayList<>();
    private boolean verticesNeedUpload = true;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
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
        // logger.info("deleting vboHandle " + vboHandle);
        vboHandle = 0;
    }

    @Override
    public void init(GL3 gl) {
        if (vboHandle != 0)
            return; // already initialized
        int [] vbos = {0};
        gl.glGenBuffers(1, vbos, 0);
        vboHandle = vbos[0];
        // logger.info("creating vboHandle " + vboHandle);
        if (floatStorage.size() > 0)
            verticesNeedUpload = true;
    }
    
    public void bind(GL3 gl, int shaderHandle) {
        if (vboHandle == 0)
            return;
        if (shaderHandle == 0)
            return;
        // logger.info("binding vboHandle " + vboHandle);
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
