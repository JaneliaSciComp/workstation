package org.janelia.it.FlyWorkstation.publication_quality.mesh.actor;

import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.opengl.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeModel;
import org.janelia.it.FlyWorkstation.gui.viewer3d.shader.AbstractShader;
import org.janelia.it.FlyWorkstation.publication_quality.mesh.VtxAttribMgr;
import org.janelia.it.FlyWorkstation.publication_quality.mesh.shader.MeshDrawShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * This is a gl-actor to draw pre-collected buffers, which have been laid out for
 * OpenGL's draw-elements.
 *
 * Created by fosterl on 4/14/14.
 */
public class MeshDrawActor implements GLActor {
    private static final String MODEL_VIEW_UNIFORM_NAME = "modelView";
    private static final String PROJECTION_UNIFORM_NAME = "projection";
    private static final String NORMAL_MATRIX_UNIFORM_NAME = "normalMatrix";

    public static final int BYTES_PER_FLOAT = Float.SIZE / Byte.SIZE;
    public static final int BYTES_PER_INT = Integer.SIZE / Byte.SIZE;

    private static Logger logger = LoggerFactory.getLogger( MeshDrawActor.class );

    static {
        try {
            GLProfile profile = GLProfile.get(GLProfile.GL3);
            final GLCapabilities capabilities = new GLCapabilities(profile);
            capabilities.setGLProfile( profile );
            // KEEPING this for use of GL3 under MAC.  So far, unneeded, and not debugged.
            //        SwingUtilities.invokeLater(new Runnable() {
            //            public void run() {
            //                new JOCLSimpleGL3(capabilities);
            //            }
            //        });
        } catch ( Throwable th ) {
            logger.error( "No GL3 profile available" );
        }

    }

    private boolean bBuffersNeedUpload = true;
    private boolean bIsInitialized;
    private int inxBufferHandle;
    private int vtxAttribBufferHandle = -1;
    private int vertexAttributeLoc = -1;
    private int normalAttributeLoc = -1;
    private BoundingBox3d boundingBox;
    private int indexCount;

    private MeshDrawActorConfigurator configurator;

    private AbstractShader shader;

    private IntBuffer tempBuffer = IntBuffer.allocate(1);

    public MeshDrawActor( MeshDrawActorConfigurator configurator ) {
        this.configurator = configurator;
    }

    /**
     * Populate this will all setters, to prepare for drawing a precomputed mesh.
     * This is a simplifying bag-o-data, to cut down on the feed into the constructor,
     * and allow some checking as needed.
     */
    public static class MeshDrawActorConfigurator {
        private VolumeModel volumeModel;
        private Long renderableId = -1L;
        private VtxAttribMgr vtxAttribMgr;
        private double[] axisLengths;

        public void setAxisLengths( double[] axisLengths ) {
            this.axisLengths = axisLengths;
        }

        public void setVolumeModel( VolumeModel model ) {
            this.volumeModel = model;
        }

        public void setRenderableId( Long renderableId ) {
            this.renderableId = renderableId;
        }

        public void setVertexAttribMgr( VtxAttribMgr vertexAttribMgr ) {
            this.vtxAttribMgr = vertexAttribMgr;
        }

        public VolumeModel getVolumeModel() {
            assert volumeModel != null : "Volume Model not initialized";
            return volumeModel;
        }

        public Long getRenderableId() {
            assert renderableId != -1 : "Renderable id unset";
            return renderableId;
        }

        public VtxAttribMgr getVtxAttribMgr() {
            assert vtxAttribMgr != null : "Attrib mgr not initialized.";
            return vtxAttribMgr;
        }

        public double[] getAxisLengths() {
            assert axisLengths != null : "Axis lengths not initialized";
            return axisLengths;
        }

    }

    @Override
    public void init(GLAutoDrawable glDrawable) {
        GL2GL3 gl = glDrawable.getGL().getGL2GL3();

        if (bBuffersNeedUpload) {
            try {
                // Uploading buffers sufficient to draw the mesh.
                //   Gonna dance this mesh a-round...
                initializeShaderValues(gl);
                uploadBuffers(gl);

                bBuffersNeedUpload = false;
            } catch ( Exception ex ) {
                SessionMgr.getSessionMgr().handleException( ex );
            }
        }

        // tidy up
        bIsInitialized = true;
    }

    @Override
    public void display(GLAutoDrawable glDrawable) {
        GL2GL3 gl = glDrawable.getGL().getGL2GL3();
        reportError( gl, "Display of mesh-draw-actor upon entry" );

        gl.glDisable(GL2GL3.GL_CULL_FACE);
        gl.glFrontFace(GL2GL3.GL_CW);
        reportError( gl, "Display of mesh-draw-actor cull-face" );

        // set blending to enable transparent voxels
        gl.glEnable(GL2GL3.GL_BLEND);
        gl.glBlendEquation(GL2GL3.GL_FUNC_ADD);
        // Weight source by GL_ONE because we are using premultiplied alpha.
        gl.glBlendFunc(GL2GL3.GL_ONE, GL2GL3.GL_ONE_MINUS_SRC_ALPHA);
        reportError( gl, "Display of axes-actor alpha" );
//        }
//        else if (renderMethod == RenderMethod.MAXIMUM_INTENSITY) {
//            gl.glBlendEquation(GL2GL3.GL_MAX);
//            gl.glBlendFunc(GL2GL3.GL_ONE, GL2GL3.GL_DST_ALPHA);
//            reportError( gl, "Display of axes-actor maxintensity" );
//        }

        // Draw the little triangles.
        tempBuffer.rewind();
        gl.glGetIntegerv(GL2.GL_CURRENT_PROGRAM, tempBuffer);
        int oldProgram = tempBuffer.get();

        gl.glUseProgram( shader.getShaderProgram() );
        gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vtxAttribBufferHandle);
        reportError( gl, "Display of mesh-draw-actor 1" );

        shader.setUniformMatrix4v( gl, PROJECTION_UNIFORM_NAME, false, configurator.getVolumeModel().getPerspectiveMatrix() );
        shader.setUniformMatrix4v( gl, MODEL_VIEW_UNIFORM_NAME, false, configurator.getVolumeModel().getModelViewMatrix() );
        shader.setUniformMatrix4v( gl, NORMAL_MATRIX_UNIFORM_NAME, false, computeNormalMatrix() );

        // TODO : make it possible to establish an arbitrary group of vertex attributes programmatically.
        // 3 floats per coord. Stride is 1 normal (3 floats=3 coords), offset to first is 0.
        gl.glEnableVertexAttribArray(vertexAttributeLoc);
        gl.glVertexAttribPointer(vertexAttributeLoc, 3, GL2.GL_FLOAT, false, 3 * BYTES_PER_FLOAT, 0);
        reportError( gl, "Display of mesh-draw-actor 2" );

        // 3 floats per normal. Stride is 1 vertex loc (3 floats=3 coords), offset to first is 1 vertex worth.
        gl.glEnableVertexAttribArray(normalAttributeLoc);
        gl.glVertexAttribPointer(normalAttributeLoc, 3, GL2.GL_FLOAT, false, 3 * BYTES_PER_FLOAT, 3 * BYTES_PER_FLOAT);
        reportError( gl, "Display of mesh-draw-actor 2a" );

        gl.glBindBuffer( GL2.GL_ELEMENT_ARRAY_BUFFER, inxBufferHandle );
        reportError(gl, "Display of mesh-draw-actor 3.");

        setColoring( gl );

        gl.glDrawElements( GL2.GL_TRIANGLES, indexCount, GL2.GL_UNSIGNED_INT, 0 );
        reportError( gl, "Display of mesh-draw-actor 4" );

        gl.glUseProgram( oldProgram );

        gl.glDisable(GL2.GL_BLEND);
        reportError(gl, "mesh-draw-actor, end of display.");

    }

    @Override
    public BoundingBox3d getBoundingBox3d() {
        if ( boundingBox == null ) {
            setupBoundingBox();
        }
        return boundingBox;
    }

    @Override
    public void dispose(GLAutoDrawable glDrawable) {

    }

    /** Because the world matrix can change, it is necessary to use it to affect normals. */
    private float[] computeNormalMatrix() {
        float[] rtnVal = new float[ 16 ];
        // Want to use only the top/left 3x3 of the model-view matrix.
        //  Pg 402 of "Interactive Computer Graphics A Top-Down Shader Based Approach with OpenGL"
        float[] normalPart = new float[16];
        System.arraycopy( configurator.getVolumeModel().getModelViewMatrix(), 0, normalPart, 0, 16 );
        normalPart[ 3 ] = 0.0f;
        normalPart[ 7 ] = 0.0f;
        normalPart[ 11 ] = 0.0f;
        normalPart[ 12 ] = 0.0f;
        normalPart[ 13 ] = 0.0f;
        normalPart[ 14 ] = 0.0f;
        normalPart[ 15 ] = 1.0f;

        float[] temp2 = new float[ 16 ];
        boolean couldInvert = MeshDrawActor.invertM(temp2, 0, normalPart, 0);
        if (! couldInvert ) {
            logger.error("ModelPos-Inv", "Could not invert matrix");
            System.arraycopy( rtnVal, 0, configurator.getVolumeModel().getModelViewMatrix(), 0, 16 );
        }
        else {
            MeshDrawActor.transposeM(rtnVal, 0, temp2, 0);
        }

        return rtnVal;
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

    private void initializeShaderValues(GL2GL3 gl) {
        try {
            shader = new MeshDrawShader();
            shader.init( gl.getGL2() );
            //shader.load( gl.getGL2() );

            vertexAttributeLoc = gl.glGetAttribLocation(shader.getShaderProgram(), MeshDrawShader.VERTEX_ATTRIBUTE_NAME);
            reportError( gl, "Obtaining the in-shader locations-1." );
            normalAttributeLoc = gl.glGetAttribLocation(shader.getShaderProgram(), MeshDrawShader.NORMAL_ATTRIBUTE_NAME);
            reportError( gl, "Obtaining the in-shader locations-2." );
            setColoring(gl);


        } catch ( AbstractShader.ShaderCreationException sce ) {
            sce.printStackTrace();
            throw new RuntimeException( sce );
        }

    }

    /** Use axes from caller to establish the bounding box. */
    private void setupBoundingBox() {
        BoundingBox3d result = new BoundingBox3d();
        Vec3 half = new Vec3(0,0,0);
        for ( int i = 0; i < 3; i++ ) {
            half.set( i, 0.5 * configurator.getAxisLengths()[ i ] );
        }

        result.include(half.minus());
        result.include(half);
        boundingBox = result;
    }

    private void setColoring(GL2GL3 gl) {
        // Must upload the color value for display, at init time.
        float grayValue = 0.15f;
        boolean wasSet = shader.setUniform4v(gl, MeshDrawShader.COLOR_UNIFORM_NAME, 1, new float[]{
                grayValue * 2.0f, grayValue, grayValue, 1.0f
        });
        if ( ! wasSet ) {
            logger.error("Failed to set the " + MeshDrawShader.COLOR_UNIFORM_NAME + " to desired value.");
        }

        reportError( gl, "Set coloring." );
    }

    private void uploadBuffers(GL2GL3 gl) {
        // Push the coords over to GPU.
        // Make handles for subsequent use.
        int[] handleArr = new int[ 1 ];
        gl.glGenBuffers( 1, handleArr, 0 );
        vtxAttribBufferHandle = handleArr[ 0 ];

        gl.glGenBuffers( 1, handleArr, 0 );
        inxBufferHandle = handleArr[ 0 ];

        // Bind data to the handle, and upload it to the GPU.
        gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vtxAttribBufferHandle);
        reportError( gl, "Bind buffer" );
        VtxAttribMgr.RenderBuffersBean buffersBean =
                configurator.getVtxAttribMgr()
                        .getRenderIdToBuffers()
                        .get(configurator.getRenderableId());
        FloatBuffer attribBuffer = buffersBean.getAttributesBuffer();
        long bufferBytes = (long) (attribBuffer.capacity() * (BYTES_PER_FLOAT));
        attribBuffer.rewind();
        gl.glBufferData(
                GL2GL3.GL_ARRAY_BUFFER,
                bufferBytes,
                attribBuffer,
                GL2GL3.GL_STATIC_DRAW
        );
        reportError( gl, "Buffer Data" );

        IntBuffer inxBuf = buffersBean.getIndexBuffer();
        indexCount = inxBuf.capacity();
        gl.glBindBuffer( GL2GL3.GL_ELEMENT_ARRAY_BUFFER, inxBufferHandle );
        reportError(gl, "Bind Inx Buf");

        gl.glBufferData(
                GL2GL3.GL_ELEMENT_ARRAY_BUFFER,
                (long)(inxBuf.capacity() * BYTES_PER_INT),
                inxBuf,
                GL2GL3.GL_STATIC_DRAW
        );

        configurator.getVtxAttribMgr().close();
    }

    private void reportError(GL gl, String source) {
        int errNum = gl.glGetError();
        if ( errNum > 0 ) {
            logger.warn(
                    "Error {}/0x0{} encountered in " + source,
                    errNum, Integer.toHexString(errNum)
            );
        }
    }

}
