package org.janelia.it.workstation.gui.viewer3d.mesh.actor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.gui.viewer3d.matrix_support.ViewMatrixSupport;
import org.janelia.it.workstation.gui.viewer3d.shader.AbstractShader;
import org.janelia.it.jacs.shared.mesh_loader.RenderBuffersBean;
import org.janelia.it.workstation.gui.viewer3d.mesh.shader.MeshDrawShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.Map;
import org.janelia.it.jacs.shared.mesh_loader.VertexAttributeSourceI;
import org.janelia.it.workstation.gui.viewer3d.MeshViewContext;
import org.janelia.it.workstation.gui.viewer3d.matrix_support.MatrixManager;

/**
 * This is a gl-actor to draw pre-collected buffers, which have been laid out for
 * OpenGL's draw-elements.
 *
 * Created by fosterl on 4/14/14.
 */
public class MeshDrawActor implements GLActor {
    public enum MatrixScope { LOCAL, EXTERNAL }
    // Set a uniform, and color everything the same way, vs
    // have a color attribute for each vertex.
    public enum ColoringStrategy { UNIFORM, ATTRIBUTE }
    
    private static final String MODEL_VIEW_UNIFORM_NAME = "modelView";
    private static final String PROJECTION_UNIFORM_NAME = "projection";
    private static final String NORMAL_MATRIX_UNIFORM_NAME = "normalMatrix";

    public static final int BYTES_PER_FLOAT = Float.SIZE / Byte.SIZE;
    public static final int BYTES_PER_INT = Integer.SIZE / Byte.SIZE;

    private static Logger logger = LoggerFactory.getLogger( MeshDrawActor.class );

    private boolean bBuffersNeedUpload = true;
    private boolean bIsInitialized;
    private int inxBufferHandle;
    private int vtxAttribBufferHandle = -1;
    private int vertexAttributeLoc = -1;
    private int normalAttributeLoc = -1;
    private int colorAttributeLoc = -1;
    
    private int indexCount;

    private MeshDrawActorConfigurator configurator;

    private MeshDrawShader shader;

    private IntBuffer tempBuffer = IntBuffer.allocate(1);
    private MatrixManager matrixManager;

    public MeshDrawActor( MeshDrawActorConfigurator configurator ) {
        this.configurator = configurator;
    }

    /**
     * Populate this will all setters, to prepare for drawing a precomputed mesh.
     * This is a simplifying bag-o-data, to cut down on the feed into the constructor,
     * and allow some checking as needed.
     */
    public static class MeshDrawActorConfigurator {
        private MeshViewContext context;
        private Long renderableId = -1L;
        private VertexAttributeSourceI vtxAttribMgr;
        private double[] axisLengths;
        private MatrixScope matrixScope = MatrixScope.EXTERNAL;
        private ColoringStrategy coloringStrategy = ColoringStrategy.UNIFORM;
        private BoundingBox3d boundingBox;

        public void setAxisLengths( double[] axisLengths ) {
            this.axisLengths = axisLengths;
        }

        public void setContext( MeshViewContext context ) {
            this.context = context;
        }

        /**
         * @return the coloringStrategy
         */
        public ColoringStrategy getColoringStrategy() {
            return coloringStrategy;
        }

        /**
         * Tell if color will be set by pushing a uniform to shader, or if
         * instead it will be seen in the attributes, as a color-per-vertex.
         *
         * @param coloringStrategy the coloringStrategy to set
         */
        public void setColoringStrategy(ColoringStrategy coloringStrategy) {
            this.coloringStrategy = coloringStrategy;
        }

        public void setRenderableId( Long renderableId ) {
            this.renderableId = renderableId;
        }

        public void setVertexAttributeManager(VertexAttributeSourceI vertexAttribMgr) {
            this.vtxAttribMgr = vertexAttribMgr;
        }

        public MeshViewContext getContext() {
            assert context != null : "Context not initialized";
            return context;
        }

        public Long getRenderableId() {
            assert renderableId != -1 : "Renderable id unset";
            return renderableId;
        }

        public VertexAttributeSourceI getVertexAttributeManager() {
            assert vtxAttribMgr != null : "Attrib mgr not initialized.";
            return vtxAttribMgr;
        }

        public double[] getAxisLengths() {
            assert axisLengths != null : "Axis lengths not initialized";
            return axisLengths;
        }

        /**
         * @return the matrixScope
         */
        public MatrixScope getMatrixScope() {
            return matrixScope;
        }

        /**
         * @param matrixScope the matrixScope to set
         */
        public void setMatrixScope(MatrixScope matrixScope) {
            this.matrixScope = matrixScope;
        }

        /**
         * @return the boundingBox
         */
        public BoundingBox3d getBoundingBox() {
            return boundingBox;
        }

        /**
         * @param boundingBox the boundingBox to set
         */
        public void setBoundingBox(BoundingBox3d boundingBox) {
            this.boundingBox = boundingBox;
        }

    }

    @Override
    public void init(final GLAutoDrawable glDrawable) {
        GL2GL3 gl = glDrawable.getGL().getGL2GL3();

        MatrixManager.WindowDef windowDef = new MatrixManager.WindowDef() {
            @Override
            public int getWidthInPixels() {
                return glDrawable.getWidth();
            }

            @Override
            public int getHeightInPixels() {
                return glDrawable.getHeight();
            }
            
        };
        
        if (bBuffersNeedUpload) {
            try {
                bBuffersNeedUpload = false;
                configurator.getVertexAttributeManager().execute();
                if (configurator.getMatrixScope() == MatrixScope.LOCAL) {
                    this.matrixManager = new MatrixManager(
                            configurator.getContext(),
                            windowDef,
                            MatrixManager.FocusBehavior.DYNAMIC // *** TEMP ***
                    );
                }
                // Uploading buffers sufficient to draw the mesh.
                //   Gonna dance this mesh a-round...
                initializeShaderValues(gl);
                uploadBuffers(gl);

            } catch ( Exception ex ) {
                SessionMgr.getSessionMgr().handleException( ex );
            }
        }

        // tidy up
        bIsInitialized = true;
    }

    @Override
    public void display(GLAutoDrawable glDrawable) {
        if (bBuffersNeedUpload) {
            init(glDrawable);
        }
        GL2GL3 gl = glDrawable.getGL().getGL2GL3();
        reportError(gl, "Display of mesh-draw-actor upon entry");

        gl.glEnable(GL2GL3.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2GL3.GL_LESS);

        gl.glFrontFace(GL2.GL_CCW);
        gl.glEnable(GL2.GL_CULL_FACE);

        reportError( gl, "Display of mesh-draw-actor render characteristics" );

        // Draw the little triangles.
        tempBuffer.rewind();
        gl.glGetIntegerv(GL2.GL_CURRENT_PROGRAM, tempBuffer);
        int oldProgram = tempBuffer.get();

        gl.glUseProgram( shader.getShaderProgram() );
        gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vtxAttribBufferHandle);
        reportError( gl, "Display of mesh-draw-actor 1" );
        
        if (matrixManager != null)
            matrixManager.recalculate(gl);

        ViewMatrixSupport vms = new ViewMatrixSupport();
        MeshDrawShader mdShader = shader;
        final MeshViewContext context = configurator.getContext();
        mdShader.setUniformMatrix4v(gl, PROJECTION_UNIFORM_NAME, false, context.getPerspectiveMatrix());
        mdShader.setUniformMatrix4v(gl, MODEL_VIEW_UNIFORM_NAME, false, context.getModelViewMatrix());
        mdShader.setUniformMatrix4v(gl, NORMAL_MATRIX_UNIFORM_NAME, false, vms.computeNormalMatrix(context.getModelViewMatrix()));
        reportError(gl, "Pushing matrix uniforms.");
        shader.setColorByAttribute(gl, true);
        reportError(gl, "Telling shader to use attribute coloring.");

        // TODO : make it possible to establish an arbitrary group of vertex attributes programmatically.
        // 3 floats per coord. Stride is 1 normal (3 floats=3 coords), offset to first is 0.
        int numberFloatsInStride = 6;
        if (configurator.getColoringStrategy() == ColoringStrategy.ATTRIBUTE) {
            numberFloatsInStride += 3;
        }
        int stride = numberFloatsInStride * BYTES_PER_FLOAT;
        logger.debug("Stride for upload is " + stride);
        int storagePerVertex = 3 * BYTES_PER_FLOAT;
        int storagePerVertexNormal = 2 * storagePerVertex;

        gl.glEnableVertexAttribArray(vertexAttributeLoc);
        gl.glVertexAttribPointer(vertexAttributeLoc, 3, GL2.GL_FLOAT, false, stride, 0);
        reportError( gl, "Display of mesh-draw-actor 2" );

        // 3 floats per normal. Stride is size of all data combined, offset to first is 1 vertex worth.
        gl.glEnableVertexAttribArray(normalAttributeLoc);
        gl.glVertexAttribPointer(normalAttributeLoc, 3, GL2.GL_FLOAT, false, stride, storagePerVertex);
        reportError( gl, "Display of mesh-draw-actor 3" );

        if (configurator.getColoringStrategy() == ColoringStrategy.ATTRIBUTE) {
            logger.debug("Also doing color attribute.");
            // 3 floats per color. Stride is size of all data combined, offset to first is 1 vertex + 1 normal worth.
            gl.glEnableVertexAttribArray(colorAttributeLoc);
            gl.glVertexAttribPointer(colorAttributeLoc, 3, GL2.GL_FLOAT, false, stride, storagePerVertexNormal);
            reportError(gl, "Display of mesh-draw-actor 3-opt");

        }
        gl.glBindBuffer( GL2.GL_ELEMENT_ARRAY_BUFFER, inxBufferHandle );
        reportError(gl, "Display of mesh-draw-actor 4.");

        // One triangle every three indices.  But count corresponds to the number of vertices.
        gl.glDrawElements( GL2.GL_TRIANGLES, indexCount, GL2.GL_UNSIGNED_INT, 0 );
        reportError( gl, "Display of mesh-draw-actor 5" );

        gl.glUseProgram( oldProgram );

        reportError(gl, "mesh-draw-actor, end of display.");
        gl.glDisable( GL2.GL_DEPTH_TEST );

    }

    @Override
    public BoundingBox3d getBoundingBox3d() {
        if ( configurator.getBoundingBox() == null ) {
            setupBoundingBox();
        }
        return configurator.getBoundingBox();
    }

    @Override
    public void dispose(GLAutoDrawable glDrawable) {

    }
    
    public void refresh() {
        bBuffersNeedUpload = true;
    }

    private void initializeShaderValues(GL2GL3 gl) {
        try {
            shader = new MeshDrawShader();
            shader.init( gl.getGL2() );

            vertexAttributeLoc = gl.glGetAttribLocation(shader.getShaderProgram(), MeshDrawShader.VERTEX_ATTRIBUTE_NAME);
            reportError( gl, "Obtaining the in-shader locations-1." );
            normalAttributeLoc = gl.glGetAttribLocation(shader.getShaderProgram(), MeshDrawShader.NORMAL_ATTRIBUTE_NAME);
            reportError(gl, "Obtaining the in-shader locations-2.");

            if (configurator.getColoringStrategy() == ColoringStrategy.ATTRIBUTE) {
                colorAttributeLoc = gl.glGetAttribLocation(shader.getShaderProgram(), MeshDrawShader.COLOR_ATTRIBUTE_NAME);
                reportError(gl, "Obtaining the in-shader locations-3.");

            } else {
                setColoring(gl);
            }

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
        configurator.setBoundingBox(result);
    }

    private void setColoring(GL2GL3 gl) {
        // Must upload the color value for display, at init time.
        //TODO get a meaningful coloring.
        this.tempBuffer.rewind();
        gl.glGetIntegerv(GL2GL3.GL_CURRENT_PROGRAM, tempBuffer);
        tempBuffer.rewind();
        int oldShader = tempBuffer.get();
        
        gl.glUseProgram(shader.getShaderProgram());
        boolean wasSet = ((MeshDrawShader) shader).setUniform4v(gl, MeshDrawShader.COLOR_UNIFORM_NAME, 1, new float[]{
            1.0f, 0.5f, 0.25f, 1.0f
        });
        if (!wasSet) {
            logger.error("Failed to set the {} to desired value.", MeshDrawShader.COLOR_UNIFORM_NAME);
        }
        gl.glUseProgram(oldShader);

        reportError( gl, "Set coloring." );
    }
    
    /** This uploads a simplistic single triangle for testing. */
    private void uploadBuffers(GL2GL3 gl, boolean flag) {
 
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
            offset, offset* 3, -offset,
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
            0.0f, 1.0f, 1.0f,
        };
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
        vertexAttribBuffer.put( vtxData );

        //dumpFloatBuffer(vertexAttribBuffer);
        
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
            20, 19, 24,
        };
        final int combinedIndexSize = indices.length * BYTES_PER_INT;
        byteBuffer = ByteBuffer.allocateDirect(combinedIndexSize);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.rewind();
        IntBuffer indexBuffer = byteBuffer.asIntBuffer();
        indexBuffer.put( indices );
        indexBuffer.rewind();
        bufferBytes = indexBuffer.capacity() * BYTES_PER_INT;
        indexCount = indexBuffer.capacity();
        logger.info("Index Count = " + indexCount);
        //dumpIntBuffer(indexBuffer);
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

    private void dumpFloatBuffer(FloatBuffer attribBuffer) {
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

    private void dumpIntBuffer(IntBuffer inxBuf) {
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
    
    private void uploadBuffers(GL2GL3 gl) {
        dropBuffers(gl);
        // Push the coords over to GPU.
        // Make handles for subsequent use.
        int[] handleArr = new int[ 1 ];
        gl.glGenBuffers( 1, handleArr, 0 );
        vtxAttribBufferHandle = handleArr[ 0 ];

        gl.glGenBuffers( 1, handleArr, 0 );
        inxBufferHandle = handleArr[ 0 ];

        reportError( gl, "Bind buffer" );
        final Map<Long, RenderBuffersBean> renderIdToBuffers =
                configurator.getVertexAttributeManager().getRenderIdToBuffers();
        long combinedVtxSize = 0L;
        long combinedInxSize = 0L;
        
        // One pass for size.
        for ( Long renderId: renderIdToBuffers.keySet() ) {
            RenderBuffersBean buffersBean = renderIdToBuffers.get(renderId);
            FloatBuffer attribBuffer = buffersBean.getAttributesBuffer();            
            if (attribBuffer != null  &&  attribBuffer.capacity() > 0) {
                long bufferBytes = (long) (attribBuffer.capacity() * (BYTES_PER_FLOAT));
                combinedVtxSize += bufferBytes;

                IntBuffer inxBuf = buffersBean.getIndexBuffer();
                bufferBytes = inxBuf.capacity() * BYTES_PER_INT;
                combinedInxSize += bufferBytes;
                logger.info("Found attributes for {}.", renderId);
            }
            else {
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
        gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vtxAttribBufferHandle);
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
        gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, inxBufferHandle);
        gl.glBufferData(
                GL2GL3.GL_ELEMENT_ARRAY_BUFFER,
                combinedInxSize,
                bean.getIndexBuffer(),  // null
                GL2GL3.GL_STATIC_DRAW
        );
        reportError(gl, "Allocate Index Buffer");
if (0==0)return;       // TEMP

        long verticesOffset = 0;
        long indicesOffset = 0;
        logger.info("Buffers allocated.");
        for ( Long renderId: renderIdToBuffers.keySet() ) {
            RenderBuffersBean buffersBean = renderIdToBuffers.get( renderId );
            FloatBuffer attribBuffer = buffersBean.getAttributesBuffer();
            if (attribBuffer != null  &&  attribBuffer.capacity() > 0) {
                long bufferBytes = (long) (attribBuffer.capacity() * (BYTES_PER_FLOAT));
                attribBuffer.rewind();
                gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vtxAttribBufferHandle);
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
                indexCount += inxBuf.capacity();
                bufferBytes = (long) (inxBuf.capacity() * BYTES_PER_INT);

                gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, inxBufferHandle);
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

        //configurator.getVertexAttributeManager().close();
    }

    protected void dropBuffers(GL2GL3 gl) {
        if (vtxAttribBufferHandle > -1) {
            gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vtxAttribBufferHandle);
            tempBuffer.rewind();
            tempBuffer.put(vtxAttribBufferHandle);
            tempBuffer.rewind();
            gl.glDeleteBuffers(1, tempBuffer);
            gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);
        }
        if (inxBufferHandle > -1) {
            gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, inxBufferHandle);
            tempBuffer.rewind();
            tempBuffer.put(inxBufferHandle);
            tempBuffer.rewind();
            gl.glDeleteBuffers(1, tempBuffer);
            gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, 0);
        }
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
