/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.viewer3d.mesh.actor;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.Map;
import javax.media.opengl.GL2GL3;
import org.janelia.it.jacs.shared.mesh_loader.RenderBuffersBean;
import static org.janelia.it.workstation.gui.viewer3d.mesh.actor.MeshDrawActor.BYTES_PER_FLOAT;
import static org.janelia.it.workstation.gui.viewer3d.mesh.actor.MeshDrawActor.BYTES_PER_INT;
import static org.janelia.it.workstation.gui.viewer3d.OpenGLUtils.reportError;
import org.janelia.it.workstation.gui.viewer3d.mesh.actor.MeshDrawActor.MeshDrawActorConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation pushes buffers from a vertex attribute manager.
 *
 * @author fosterl
 */
public class AttributeManagerBufferUploader implements BufferUploader {
    private static final Logger logger = LoggerFactory.getLogger(AttributeManagerBufferUploader.class);
    private int vtxAttribBufferHandle;
    private int inxBufferHandle;
    private int indexCount;
    
    private final MeshDrawActorConfigurator configurator;
    
    public AttributeManagerBufferUploader( MeshDrawActorConfigurator configurator ) {
        this.configurator = configurator;
    }
    
    @Override
    public void uploadBuffers(GL2GL3 gl) {
        // Push the coords over to GPU.
        // Make handles for subsequent use.
        int[] handleArr = new int[2];
        gl.glGenBuffers(2, handleArr, 0);        
        this.vtxAttribBufferHandle = handleArr[0];
        this.inxBufferHandle = handleArr[1];

        reportError(gl, "Bind buffer");
        final Map<Long, RenderBuffersBean> renderIdToBuffers
                = configurator.getVertexAttributeManager().getRenderIdToBuffers();
        long combinedVtxSize = 0L;
        long combinedInxSize = 0L;

        // One pass for size.
        for (Long renderId : renderIdToBuffers.keySet()) {
            RenderBuffersBean buffersBean = renderIdToBuffers.get(renderId);
            FloatBuffer attribBuffer = buffersBean.getAttributesBuffer();
            if (attribBuffer != null && attribBuffer.capacity() > 0) {
                long bufferBytes = (long) (attribBuffer.capacity() * (BYTES_PER_FLOAT));
                combinedVtxSize += bufferBytes;

                IntBuffer inxBuf = buffersBean.getIndexBuffer();
                bufferBytes = inxBuf.capacity() * BYTES_PER_INT;
                combinedInxSize += bufferBytes;
                logger.info("Found attributes for {}.", renderId);
            } else {
                logger.warn("No attributes for renderer id: {}.", renderId);
            }
        }

        logger.info("Allocating buffers");

        // Allocate enough remote buffer data for all the vertices/attributes
        // to be thrown across in segments.
        final Iterator<RenderBuffersBean> iterator = renderIdToBuffers.values().iterator();
        iterator.next();
        RenderBuffersBean bean = iterator.next();
//dumpFloatBuffer(bean.getAttributesBuffer());
//dumpIntBuffer(bean.getIndexBuffer());
        bean.getAttributesBuffer().rewind();
        gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, getVtxAttribBufferHandle());
        gl.glBufferData(
                GL2GL3.GL_ARRAY_BUFFER,
                combinedVtxSize,
                bean.getAttributesBuffer(), // null
                GL2GL3.GL_STATIC_DRAW
        );
        reportError(gl, "Allocate Vertex Buffer");

        // Allocate enough remote buffer data for all the indices to be
        // thrown across in segments.
        indexCount = bean.getIndexBuffer().capacity();
        bean.getIndexBuffer().rewind();
        gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, getInxBufferHandle());
        gl.glBufferData(
                GL2GL3.GL_ELEMENT_ARRAY_BUFFER,
                combinedInxSize,
                bean.getIndexBuffer(), // null
                GL2GL3.GL_STATIC_DRAW
        );
        reportError(gl, "Allocate Index Buffer");
        if (0 == 0) {
            return;       // TEMP
        }
        long verticesOffset = 0;
        long indicesOffset = 0;
        logger.info("Buffers allocated.");
        for (Long renderId : renderIdToBuffers.keySet()) {
            RenderBuffersBean buffersBean = renderIdToBuffers.get(renderId);
            FloatBuffer attribBuffer = buffersBean.getAttributesBuffer();
            if (attribBuffer != null && attribBuffer.capacity() > 0) {
                long bufferBytes = (long) (attribBuffer.capacity() * (BYTES_PER_FLOAT));
                attribBuffer.rewind();
                gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, getVtxAttribBufferHandle());
                reportError(gl, "Bind Attribs Buf");
                logger.info("Uploading chunk of vertex attributes data.");
                gl.glBufferSubData(
                        GL2GL3.GL_ARRAY_BUFFER,
                        verticesOffset,
                        bufferBytes,
                        attribBuffer
                );
                verticesOffset += bufferBytes;
                reportError(gl, "Buffer Data");

                IntBuffer inxBuf = buffersBean.getIndexBuffer();
                inxBuf.rewind();
                indexCount = getIndexCount() + inxBuf.capacity();
                bufferBytes = (long) (inxBuf.capacity() * BYTES_PER_INT);

                gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, getInxBufferHandle());
                reportError(gl, "Bind Inx Buf");
                logger.info("Uploading chunk of element array.");
                gl.glBufferSubData(
                        GL2GL3.GL_ELEMENT_ARRAY_BUFFER,
                        bufferBytes,
                        indicesOffset,
                        inxBuf
                );
                indicesOffset += bufferBytes;
                reportError(gl, "Upload index buffer segment.");
            }
        }

        configurator.getVertexAttributeManager().close();
    }

    /**
     * @return the vtxAttribBufferHandle
     */
    @Override
    public int getVtxAttribBufferHandle() {
        return vtxAttribBufferHandle;
    }

    /**
     * @return the inxBufferHandle
     */
    @Override
    public int getInxBufferHandle() {
        return inxBufferHandle;
    }

    /**
     * @return the indexCount
     */
    @Override
    public int getIndexCount() {
        return indexCount;
    }

}
