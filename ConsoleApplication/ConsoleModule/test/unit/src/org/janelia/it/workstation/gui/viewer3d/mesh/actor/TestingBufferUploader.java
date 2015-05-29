/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.viewer3d.mesh.actor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import javax.media.opengl.GL2GL3;
import static org.janelia.it.workstation.gui.viewer3d.OpenGLUtils.reportError;
import static org.janelia.it.workstation.gui.viewer3d.mesh.actor.MeshDrawActor.BYTES_PER_FLOAT;
import static org.janelia.it.workstation.gui.viewer3d.mesh.actor.MeshDrawActor.BYTES_PER_INT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author fosterl
 */
public class TestingBufferUploader implements BufferUploader {
    private int vtxAttribBufferHandle;
    private int inxBufferHandle;
    private int indexCount;
    
    private static final Logger logger = LoggerFactory.getLogger( TestingBufferUploader.class );
    
    /**
     * This uploads a simplistic structure for testing.
     */
    @Override
    public void uploadBuffers(GL2GL3 gl) {

        logger.info("Uploading buffers");
        //dropBuffers(gl);

        int[] handleArr = new int[1];
        gl.glGenBuffers(1, handleArr, 0);
        vtxAttribBufferHandle = handleArr[ 0];

        gl.glGenBuffers(1, handleArr, 0);
        inxBufferHandle = handleArr[ 0];
        // Borrowed from the successful upload method.
        float offset = 20.0f;
        final float[] vtxData = new float[]{
            -offset, -offset, offset,
            -0.57735026f, -0.57735026f, 0.57735026f,
            0.0f, 1.0f, 1.0f,
            -offset, offset, offset,
            -0.70710677f, 0.0f, 0.70710677f,
            0.0f, 1.0f, 1.0f,
            -offset, offset, -offset,
            -0.70710677f, 0.0f, -0.70710677f,
            0.0f, 1.0f, 1.0f,
            -offset, -offset, -offset,
            -0.57735026f, -0.57735026f, -0.57735026f,
            0.0f, 1.0f, 1.0f,
            offset, offset, offset,
            0.0f, -1.0f, 0.0f,
            0.0f, 1.0f, 1.0f,
            offset, -offset, offset,
            0.57735026f, -0.57735026f, 0.57735026f,
            0.0f, 1.0f, 1.0f,
            offset, -offset, -offset,
            0.57735026f, -0.57735026f, -0.57735026f,
            0.0f, 1.0f, 1.0f,
            offset, offset, -offset,
            0.70710677f, 0.0f, -0.70710677f,
            0.0f, 1.0f, 1.0f,
            -offset, offset * 3, offset,
            -0.57735026f, 0.57735026f, 0.57735026f,
            0.0f, 1.0f, 1.0f,
            offset, offset * 3, offset,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 1.0f,
            offset, offset * 3, -offset,
            0.57735026f, 0.57735026f, -0.57735026f,
            0.0f, 1.0f, 1.0f,
            -offset, offset * 3, -offset,
            -0.57735026f, 0.57735026f, -0.57735026f,
            0.0f, 1.0f, 1.0f,
            offset, offset * 3, offset * 3,
            -0.57735026f, 0.57735026f, 0.57735026f,
            0.0f, 1.0f, 1.0f,
            offset * 3, offset * 3, offset * 3,
            0.57735026f, 0.57735026f, 0.57735026f,
            0.0f, 1.0f, 1.0f,
            offset * 3, offset * 3, offset,
            0.57735026f, 0.57735026f, -0.57735026f,
            0.0f, 1.0f, 1.0f,
            offset, offset, offset * 3,
            -0.57735026f, -0.57735026f, 0.57735026f,
            0.0f, 1.0f, 1.0f,
            offset * 3, offset, offset * 3,
            0.57735026f, -0.57735026f, 0.57735026f,
            0.0f, 1.0f, 1.0f,
            offset * 3, offset, offset,
            0.57735026f, -0.57735026f, -0.57735026f,
            0.0f, 1.0f, 1.0f,
            -offset, offset, offset * 11,
            -0.57735026f, 0.57735026f, 0.57735026f,
            0.0f, 1.0f, 1.0f,
            offset, offset, offset * 11,
            0.57735026f, 0.57735026f, 0.57735026f,
            0.0f, 1.0f, 1.0f,
            offset, offset, offset * 9,
            0.57735026f, 0.57735026f, -0.57735026f,
            0.0f, 1.0f, 1.0f,
            -offset, offset, offset * 9,
            -0.57735026f, 0.57735026f, -0.57735026f,
            0.0f, 1.0f, 1.0f,
            -offset, -offset, offset * 11,
            -0.57735026f, -0.57735026f, 0.57735026f,
            0.0f, 1.0f, 1.0f,
            -offset, -offset, offset * 9,
            -0.57735026f, -0.57735026f, -0.57735026f,
            0.0f, 1.0f, 1.0f,
            offset, -offset, offset * 11,
            0.57735026f, -0.57735026f, 0.57735026f,
            0.0f, 1.0f, 1.0f,
            offset, -offset, offset * 9,
            0.57735026f, -0.57735026f, -0.57735026f,
            0.0f, 1.0f, 1.0f,};
        // Adjust for local matrix conditions.
        for (int i = 0; i < vtxData.length; i++) {
            if (i % 9 == 0) {
                vtxData[i] += 74000;
            }
            if (i % 9 == 1) {
                vtxData[i] += 48200;
            }
            if (i % 9 == 2) {
                vtxData[i] += 19500;
            }
        }
        /*
         */
        final int combinedVtxSize = vtxData.length * BYTES_PER_FLOAT;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(
                combinedVtxSize);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.rewind();
        FloatBuffer vertexAttribBuffer = byteBuffer.asFloatBuffer();
        vertexAttribBuffer.put(vtxData);

        dumpFloatBuffer(vertexAttribBuffer);
        vertexAttribBuffer.rewind();
        long bufferBytes = vertexAttribBuffer.capacity() * BYTES_PER_FLOAT;
        vertexAttribBuffer.rewind();
        // Bind buffer, and push buffer data.
        gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vtxAttribBufferHandle);
        gl.glBufferData(
                GL2GL3.GL_ARRAY_BUFFER,
                bufferBytes,
                vertexAttribBuffer,
                GL2GL3.GL_STATIC_DRAW
        );
        reportError(gl, "Push Vertex Buffer");

        final int[] indices = new int[]{
            0, 1, 2,
            2, 3, 0,
            4, 1, 0,
            0, 5, 4,
            5, 0, 3,
            3, 6, 5,
            6, 3, 2,
            2, 7, 6,
            5, 6, 7,
            7, 4, 5,
            8, 9, 10,
            10, 11, 8,
            1, 8, 11,
            11, 2, 1,
            9, 8, 1,
            1, 4, 9,
            7, 2, 11,
            11, 10, 7,
            4, 7, 10,
            10, 9, 4,
            12, 13, 14,
            14, 9, 12,
            15, 12, 9,
            9, 4, 15,
            13, 12, 15,
            15, 16, 13,
            16, 15, 4,
            4, 17, 16,
            17, 4, 9,
            9, 14, 17,
            16, 17, 14,
            14, 13, 16,
            18, 19, 20,
            20, 21, 18,
            22, 18, 21,
            21, 23, 22,
            19, 18, 22,
            22, 24, 19,
            24, 22, 23,
            23, 25, 24,
            25, 23, 21,
            21, 20, 25,
            24, 25, 20,
            20, 19, 24,};
        final int combinedIndexSize = indices.length * BYTES_PER_INT;
        byteBuffer = ByteBuffer.allocateDirect(combinedIndexSize);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.rewind();
        IntBuffer indexBuffer = byteBuffer.asIntBuffer();
        indexBuffer.put(indices);
        indexBuffer.rewind();
        bufferBytes = indexBuffer.capacity() * BYTES_PER_INT;
        indexCount = indexBuffer.capacity();
        logger.info("Index Count = " + indexCount);
        dumpIntBuffer(indexBuffer);
        indexBuffer.rewind();

        gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, inxBufferHandle);
        gl.glBufferData(
                GL2GL3.GL_ELEMENT_ARRAY_BUFFER,
                bufferBytes,
                indexBuffer,
                GL2GL3.GL_STATIC_DRAW
        );
        reportError(gl, "Push Index Buffer");
        logger.info("Done uploading buffers");
    }

    @Override
    public int getVtxAttribBufferHandle() {
        return this.vtxAttribBufferHandle;
    }

    @Override
    public int getInxBufferHandle() {
        return this.inxBufferHandle;
    }

    @Override
    public int getIndexCount() {
        return this.indexCount;
    }

    public void dumpFloatBuffer(FloatBuffer attribBuffer) {
        attribBuffer.rewind();
        StringBuilder bldr = new StringBuilder();
        for (int i = 0; i < attribBuffer.capacity(); i++) {
            if (i % 3 == 0) {
                bldr.append("\n");
            }
            float nextF = attribBuffer.get();
            bldr.append(nextF + "f, ");
        }
        System.out.println("[------------- Buffer Contents -------------]");
        logger.info(bldr.toString());
        attribBuffer.rewind();
    }

    public void dumpIntBuffer(IntBuffer inxBuf) {
        inxBuf.rewind();
        StringBuilder bldr = new StringBuilder();
        for (int i = 0; i < inxBuf.capacity(); i++) {
            if (i % 3 == 0) {
                bldr.append("\n");
            }
            int nextI = inxBuf.get();
            bldr.append(nextI + ", ");
        }
        System.out.println("[------------- Index Buffer Contents -------------]");
        logger.info(bldr.toString());
        inxBuf.rewind();
    }

}
