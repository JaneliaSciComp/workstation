package org.janelia.it.workstation.publication_quality.mesh.actor;

import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.gui.viewer3d.matrix_support.ViewMatrixSupport;
import org.janelia.it.workstation.gui.viewer3d.shader.AbstractShader;
import org.janelia.it.jacs.shared.mesh_loader.RenderBuffersBean;
import org.janelia.it.jacs.shared.mesh_loader.VertexAttributeManagerI;
import org.janelia.it.workstation.publication_quality.mesh.shader.MeshDrawShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.janelia.it.workstation.gui.viewer3d.MeshViewContext;

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
        private MeshViewContext context;
        private Long renderableId = -1L;
        private VertexAttributeManagerI vtxAttribMgr;
        private double[] axisLengths;

        public void setAxisLengths( double[] axisLengths ) {
            this.axisLengths = axisLengths;
        }

        public void setContext( MeshViewContext context ) {
            this.context = context;
        }

        public void setRenderableId( Long renderableId ) {
            this.renderableId = renderableId;
        }

        public void setVertexAttributeManager(VertexAttributeManagerI vertexAttribMgr) {
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

        reportError( gl, "Display of mesh-draw-actor render characteristics" );

        // Draw the little triangles.
        tempBuffer.rewind();
        gl.glGetIntegerv(GL2.GL_CURRENT_PROGRAM, tempBuffer);
        int oldProgram = tempBuffer.get();

        gl.glUseProgram( shader.getShaderProgram() );
        gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vtxAttribBufferHandle);
        reportError( gl, "Display of mesh-draw-actor 1" );

        ViewMatrixSupport vms = new ViewMatrixSupport();
        if (shader instanceof MeshDrawShader) {
            MeshDrawShader mdShader = (MeshDrawShader)shader;
            final MeshViewContext context = configurator.getContext();
            mdShader.setUniformMatrix4v(gl, PROJECTION_UNIFORM_NAME, false, context.getPerspectiveMatrix());
            mdShader.setUniformMatrix4v(gl, MODEL_VIEW_UNIFORM_NAME, false, context.getModelViewMatrix());
            mdShader.setUniformMatrix4v(gl, NORMAL_MATRIX_UNIFORM_NAME, false, vms.computeNormalMatrix(context.getModelViewMatrix()));
        }

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
        if (shader instanceof MeshDrawShader) {
            boolean wasSet = ((MeshDrawShader)shader).setUniform4v(gl, MeshDrawShader.COLOR_UNIFORM_NAME, 1, new float[]{
                1.0f, 0.5f, 0.25f, 1.0f
            });
            if (!wasSet) {
                logger.error("Failed to set the " + MeshDrawShader.COLOR_UNIFORM_NAME + " to desired value.");
            }
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
        RenderBuffersBean buffersBean =
                configurator.getVertexAttributeManager()
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
        inxBuf.rewind();
        indexCount = inxBuf.capacity();
        gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, inxBufferHandle);
        reportError(gl, "Bind Inx Buf");

        gl.glBufferData(
                GL2GL3.GL_ELEMENT_ARRAY_BUFFER,
                (long)(inxBuf.capacity() * BYTES_PER_INT),
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
