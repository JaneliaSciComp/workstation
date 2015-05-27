package org.janelia.it.workstation.publication_quality.mesh.actor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.gui.viewer3d.VolumeModel;
import org.janelia.it.workstation.gui.viewer3d.matrix_support.ViewMatrixSupport;
import org.janelia.it.workstation.gui.viewer3d.shader.AbstractShader;
import org.janelia.it.jacs.shared.loader.mesh.RenderBuffersBean;
import org.janelia.it.jacs.shared.loader.mesh.VertexAttributeManagerI;
import org.janelia.it.workstation.publication_quality.mesh.shader.MeshDrawShader;
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
        private VertexAttributeManagerI vtxAttribMgr;
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

        public void setVertexAttributeManager(VertexAttributeManagerI vertexAttribMgr) {
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

        public VertexAttributeManagerI getVertexAttributeManager() {
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
        reportError(gl, "Display of mesh-draw-actor upon entry");

        gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT | GL2GL3.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL2GL3.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2GL3.GL_LESS);
        
        gl.glDisable(GL2GL3.GL_CULL_FACE);

        reportError( gl, "Display of mesh-draw-actor render characteristics" );

        // Draw the little triangles.
        tempBuffer.rewind();
        gl.glGetIntegerv(GL2.GL_CURRENT_PROGRAM, tempBuffer);
        int oldProgram = tempBuffer.get();

        gl.glUseProgram( shader.getShaderProgram() );
        gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vtxAttribBufferHandle);
        reportError( gl, "Display of mesh-draw-actor 1" );

        ViewMatrixSupport vms = new ViewMatrixSupport();
        shader.setUniformMatrix4v( gl, PROJECTION_UNIFORM_NAME, false, configurator.getVolumeModel().getPerspectiveMatrix() );
        shader.setUniformMatrix4v( gl, MODEL_VIEW_UNIFORM_NAME, false, configurator.getVolumeModel().getModelViewMatrix() );
        shader.setUniformMatrix4v( gl, NORMAL_MATRIX_UNIFORM_NAME, false, vms.computeNormalMatrix(configurator.getVolumeModel().getModelViewMatrix()) );

        // TODO : make it possible to establish an arbitrary group of vertex attributes programmatically.
        // 3 floats per coord. Stride is 1 normal (3 floats=3 coords), offset to first is 0.
        gl.glEnableVertexAttribArray(vertexAttributeLoc);

        int stride = 6 * BYTES_PER_FLOAT;
        int storagePerVertex = 3 * BYTES_PER_FLOAT;

        gl.glVertexAttribPointer(vertexAttributeLoc, 3, GL2.GL_FLOAT, false, stride, 0);
        reportError( gl, "Display of mesh-draw-actor 2" );

        // 3 floats per normal. Stride is 1 vertex loc (3 floats=3 coords), offset to first is 1 vertex worth.
        gl.glEnableVertexAttribArray(normalAttributeLoc);
        gl.glVertexAttribPointer(normalAttributeLoc, 3, GL2.GL_FLOAT, false, stride, storagePerVertex);
        reportError( gl, "Display of mesh-draw-actor 2a" );

        gl.glBindBuffer( GL2.GL_ELEMENT_ARRAY_BUFFER, inxBufferHandle );
        reportError(gl, "Display of mesh-draw-actor 3.");

        setColoring( gl );

        // One triangle every three indices.  But count corresponds to the number of vertices.
        gl.glDrawElements( GL2.GL_TRIANGLES, indexCount, GL2.GL_UNSIGNED_INT, 0 );
        reportError( gl, "Display of mesh-draw-actor 4" );

        gl.glUseProgram( oldProgram );

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

    private void initializeShaderValues(GL2GL3 gl) {
        try {
            shader = new MeshDrawShader();
            shader.init( gl.getGL2() );

            vertexAttributeLoc = gl.glGetAttribLocation(shader.getShaderProgram(), MeshDrawShader.VERTEX_ATTRIBUTE_NAME);
            reportError( gl, "Obtaining the in-shader locations-1." );
            normalAttributeLoc = gl.glGetAttribLocation(shader.getShaderProgram(), MeshDrawShader.NORMAL_ATTRIBUTE_NAME);
            reportError(gl, "Obtaining the in-shader locations-2.");


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
        //TODO get a meaningful coloring.
        boolean wasSet = shader.setUniform4v(gl, MeshDrawShader.COLOR_UNIFORM_NAME, 1, new float[]{
                1.0f, 0.5f, 0.25f, 1.0f
        });
        if ( ! wasSet ) {
            logger.error("Failed to set the " + MeshDrawShader.COLOR_UNIFORM_NAME + " to desired value.");
        }

        reportError( gl, "Set coloring." );
    }

    /** This uploads a simplistic single triangle for testing. */
    private void uploadBuffers(GL2GL3 gl) {
 
        logger.info("Uploading buffers");
        //dropBuffers(gl);
        
        int[] handleArr = new int[1];
        gl.glGenBuffers(1, handleArr, 0);
        vtxAttribBufferHandle = handleArr[ 0];

        gl.glGenBuffers(1, handleArr, 0);
        inxBufferHandle = handleArr[ 0];
        // Borrowed from the successful upload method.
        final float[] vtxData = new float[]{
            -0.5f, -0.5f, 0.5f,
            -0.57735026f, -0.57735026f, 0.57735026f,
            -0.5f, 0.5f, 0.5f,
            -0.70710677f, 0.0f, 0.70710677f,
            -0.5f, 0.5f, -0.5f,
            -0.70710677f, 0.0f, -0.70710677f,
            -0.5f, -0.5f, -0.5f,
            -0.57735026f, -0.57735026f, -0.57735026f,
            0.5f, 0.5f, 0.5f,
            0.0f, -1.0f, 0.0f,
            0.5f, -0.5f, 0.5f,
            0.57735026f, -0.57735026f, 0.57735026f,
            0.5f, -0.5f, -0.5f,
            0.57735026f, -0.57735026f, -0.57735026f,
            0.5f, 0.5f, -0.5f,
            0.70710677f, 0.0f, -0.70710677f,
            -0.5f, 1.5f, 0.5f,
            -0.57735026f, 0.57735026f, 0.57735026f,
            0.5f, 1.5f, 0.5f,
            0.0f, 1.0f, 0.0f,
            0.5f, 1.5f, -0.5f,
            0.57735026f, 0.57735026f, -0.57735026f,
            -0.5f, 1.5f, -0.5f,
            -0.57735026f, 0.57735026f, -0.57735026f,
            0.5f, 1.5f, 1.5f,
            -0.57735026f, 0.57735026f, 0.57735026f,
            1.5f, 1.5f, 1.5f,
            0.57735026f, 0.57735026f, 0.57735026f,
            1.5f, 1.5f, 0.5f,
            0.57735026f, 0.57735026f, -0.57735026f,
            0.5f, 0.5f, 1.5f,
            -0.57735026f, -0.57735026f, 0.57735026f,
            1.5f, 0.5f, 1.5f,
            0.57735026f, -0.57735026f, 0.57735026f,
            1.5f, 0.5f, 0.5f,
            0.57735026f, -0.57735026f, -0.57735026f,
            -0.5f, 0.5f, 5.5f,
            -0.57735026f, 0.57735026f, 0.57735026f,
            0.5f, 0.5f, 5.5f,
            0.57735026f, 0.57735026f, 0.57735026f,
            0.5f, 0.5f, 4.5f,
            0.57735026f, 0.57735026f, -0.57735026f,
            -0.5f, 0.5f, 4.5f,
            -0.57735026f, 0.57735026f, -0.57735026f,
            -0.5f, -0.5f, 5.5f,
            -0.57735026f, -0.57735026f, 0.57735026f,
            -0.5f, -0.5f, 4.5f,
            -0.57735026f, -0.57735026f, -0.57735026f,
            0.5f, -0.5f, 5.5f,
            0.57735026f, -0.57735026f, 0.57735026f,
            0.5f, -0.5f, 4.5f,
            0.57735026f, -0.57735026f, -0.57735026f,
        };
        final int combinedVtxSize = vtxData.length * BYTES_PER_FLOAT;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(
                combinedVtxSize);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.rewind();
        FloatBuffer vertexAttribBuffer = byteBuffer.asFloatBuffer();
        vertexAttribBuffer.put( vtxData );

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

    private void uploadBuffers(GL2GL3 gl, boolean flag) {
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
        RenderBuffersBean buffersBean =
                configurator.getVertexAttributeManager()
                        .getRenderIdToBuffers()
                        .get(configurator.getRenderableId());
        FloatBuffer attribBuffer = buffersBean.getAttributesBuffer();
        long bufferBytes = (long) (attribBuffer.capacity() * (BYTES_PER_FLOAT));
        dumpFloatBuffer(attribBuffer);
        attribBuffer.rewind();
        gl.glBufferData(
                GL2GL3.GL_ARRAY_BUFFER,
                bufferBytes,
                attribBuffer,
                GL2GL3.GL_STATIC_DRAW
        );
        reportError( gl, "Buffer Data" );

        IntBuffer inxBuf = buffersBean.getIndexBuffer();
        dumpIntBuffer(inxBuf);
        inxBuf.rewind();
        indexCount = inxBuf.capacity();
        logger.info("Index count=" + indexCount);
        gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, inxBufferHandle);
        reportError(gl, "Bind Inx Buf");
        bufferBytes = (long)(inxBuf.capacity() * BYTES_PER_INT);
        inxBuf.rewind();
        gl.glBufferData(
                GL2GL3.GL_ELEMENT_ARRAY_BUFFER,
                bufferBytes,
                inxBuf,
                GL2GL3.GL_STATIC_DRAW
        );

        configurator.getVertexAttributeManager().close();
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
