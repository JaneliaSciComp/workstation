package org.janelia.it.workstation.gui.viewer3d.mesh.actor;

import java.awt.Dimension;
import java.awt.Toolkit;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.gui.viewer3d.matrix_support.ViewMatrixSupport;
import org.janelia.it.workstation.gui.viewer3d.shader.AbstractShader;
import org.janelia.it.workstation.gui.viewer3d.mesh.shader.MeshDrawShader;
import static org.janelia.it.workstation.gui.viewer3d.OpenGLUtils.reportError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.*;
import java.nio.IntBuffer;
import org.janelia.it.jacs.shared.mesh_loader.VertexAttributeSourceI;
import org.janelia.it.workstation.gui.viewer3d.MeshViewContext;
import org.janelia.it.workstation.gui.viewer3d.matrix_support.MatrixManager;
import org.janelia.it.workstation.gui.viewer3d.picking.RenderedIdPicker;

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
    private int vertexAttributeLoc = -1;
    private int normalAttributeLoc = -1;
    private int colorAttributeLoc = -1;
    private int idAttributeLoc = -1;
    
    private MeshDrawActorConfigurator configurator;

    private MeshDrawShader shader;

    private IntBuffer tempBuffer = IntBuffer.allocate(1);
    private MatrixManager matrixManager;
    
    private RenderedIdPicker picker;

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
        private BufferUploader bufferUploader;
        private boolean useIdAttribute;
        
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

        /**
         * If true, this directs the shader to expect pushing of a special
         * vertex attribute for the identifier of the affected vertex.
         * 
         * @return the useIdAttribute
         */
        public boolean isUseIdAttribute() {
            return useIdAttribute;
        }

        /**
         * @param useIdAttribute the useIdAttribute to set
         */
        public void setUseIdAttribute(boolean useIdAttribute) {
            this.useIdAttribute = useIdAttribute;
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

        /**
         * @return the bufferUploader
         */
        public BufferUploader getBufferUploader() {
            return bufferUploader;
        }

        /**
         * @param bufferUploader the bufferUploader to set
         */
        public void setBufferUploader(BufferUploader bufferUploader) {
            this.bufferUploader = bufferUploader;
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
                if (! initializeShaderValues(gl) ) {
                    bBuffersNeedUpload = true;
                    return;
                }
                dropBuffers(gl);
                configurator.getBufferUploader().uploadBuffers(gl);
                
                if (configurator.isUseIdAttribute()) {
//                    Toolkit toolkit = Toolkit.getDefaultToolkit();
//                    Dimension dim = toolkit.getScreenSize();
//                    // Build this with max-possible buffer dimensions.
//                    picker = new RenderedIdPicker((int)dim.getWidth(), (int)dim.getHeight());
//                    picker.init(glDrawable);
                }
            } catch ( BufferStateException bse ) {
                // Failure at this level.  Need to do this again.
                bBuffersNeedUpload = true;
            } catch ( Exception ex ) {
                SessionMgr.getSessionMgr().handleException( ex );
            }
        }
        
    }

    @Override
    public void display(GLAutoDrawable glDrawable) {
        if (! bBuffersNeedUpload && shader == null) {
            // Cover strange, overlapping-display-attempts case.
            return;
        }
        BufferUploader bufferUploader = configurator.getBufferUploader();
        if (bBuffersNeedUpload) {
            init(glDrawable);
            if (bBuffersNeedUpload) {
                // Implies the initialization failed.  Do nothing further.
                return;
            }
        }
        GL2GL3 gl = glDrawable.getGL().getGL2GL3();
        if (reportError(gl, "Display of mesh-draw-actor upon entry"))
            return;

        gl.glEnable(GL2GL3.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2GL3.GL_LESS);

        gl.glFrontFace(GL2.GL_CCW);
        gl.glEnable(GL2.GL_CULL_FACE);

        gl.glEnable(GL2.GL_LINE_SMOOTH);                     // May not be in v2
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);   // May not be in v2

        if (reportError( gl, "Display of mesh-draw-actor render characteristics" ))
            return;

        // Draw the little triangles.
        tempBuffer.rewind();
        shader.load(gl.getGL2());
        gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, bufferUploader.getVtxAttribBufferHandle());
        if (reportError( gl, "Display of mesh-draw-actor 1" ))
            return;
        
        if (matrixManager != null) {
            double far = configurator.getContext().getCameraFocusDistance() * 4.0;
            matrixManager.recalculate(gl, 10.0, far);
            //matrixManager.recalculate(gl);
        }

        ViewMatrixSupport vms = new ViewMatrixSupport();
        MeshDrawShader mdShader = shader;
        final MeshViewContext context = configurator.getContext();
        mdShader.setUniformMatrix4v(gl, PROJECTION_UNIFORM_NAME, false, context.getPerspectiveMatrix());
        mdShader.setUniformMatrix4v(gl, MODEL_VIEW_UNIFORM_NAME, false, context.getModelViewMatrix());
        mdShader.setUniformMatrix4v(gl, NORMAL_MATRIX_UNIFORM_NAME, false, vms.computeNormalMatrix(context.getModelViewMatrix()));
        if (reportError(gl, "Pushing matrix uniforms."))
            return;
        shader.setColorByAttribute(gl, configurator.getColoringStrategy().equals(ColoringStrategy.ATTRIBUTE));
        if (reportError(gl, "Telling shader to use attribute coloring.")) {
            return;
        }
        
        shader.setIdsAvailableAttribute(gl, configurator.isUseIdAttribute());
        if (reportError(gl, "Telling shader to use id attributes.")) {
            return;
        }

        // TODO : make it possible to establish an arbitrary group of vertex attributes programmatically.
        // 3 floats per coord. Stride is 1 normal (3 floats=3 coords), offset to first is 0.
        int numberFloatsInStride = 6;
        if (configurator.getColoringStrategy() == ColoringStrategy.ATTRIBUTE) {
            numberFloatsInStride += 3;
        }
        if (configurator.isUseIdAttribute()) {
            numberFloatsInStride += 3;
        }
        int stride = numberFloatsInStride * BYTES_PER_FLOAT;
        logger.debug("Stride for upload is " + stride);
        int storagePerVertex = 3 * BYTES_PER_FLOAT;
        int storagePerVertexNormal = 2 * storagePerVertex;

        gl.glEnableVertexAttribArray(vertexAttributeLoc);
        gl.glVertexAttribPointer(vertexAttributeLoc, 3, GL2.GL_FLOAT, false, stride, 0);
        if (reportError( gl, "Display of mesh-draw-actor 2" ))
            return;

        // 3 floats per normal. Stride is size of all data combined, offset to first is 1 vertex worth.
        gl.glEnableVertexAttribArray(normalAttributeLoc);
        gl.glVertexAttribPointer(normalAttributeLoc, 3, GL2.GL_FLOAT, false, stride, storagePerVertex);
        if (reportError( gl, "Display of mesh-draw-actor 3" ))
            return;

        int storagePerVertexNormalColor = 0;
        if (configurator.getColoringStrategy() == ColoringStrategy.ATTRIBUTE) {
            logger.debug("Also doing color attribute.");
            // 3 floats per color. Stride is size of all data combined, offset to first is 1 vertex + 1 normal worth.
            gl.glEnableVertexAttribArray(colorAttributeLoc);
            gl.glVertexAttribPointer(colorAttributeLoc, 3, GL2.GL_FLOAT, false, stride, storagePerVertexNormal);
            if (reportError(gl, "Display of mesh-draw-actor 3-opt"))
                return;

        }
        if (configurator.isUseIdAttribute()) {
            storagePerVertexNormalColor = 3 * storagePerVertex;
            logger.debug("Also sending IDs.");
            // 3 floats per id.
            gl.glEnableVertexAttribArray(idAttributeLoc);
            gl.glVertexAttribPointer(idAttributeLoc, 3, GL2.GL_FLOAT, false, stride, storagePerVertexNormalColor);
            if (reportError(gl, "Display of mesh-draw-actor 4-opt")) {
                return;
            }
            
        }
        gl.glBindBuffer( GL2.GL_ELEMENT_ARRAY_BUFFER, bufferUploader.getInxBufferHandle() );
        if (reportError(gl, "Display of mesh-draw-actor 4."))
            return;

        // One triangle every three indices.  But count corresponds to the number of vertices.
        gl.glDrawElements( GL2.GL_TRIANGLES, bufferUploader.getIndexCount(), GL2.GL_UNSIGNED_INT, 0 );
        if (reportError( gl, "Display of mesh-draw-actor 5" ))
            return;

//        if (configurator.isUseIdAttribute()) {
//            picker.postPick(glDrawable);
//        }
        
        shader.unload(gl.getGL2());

        if (reportError(gl, "mesh-draw-actor, end of display."))
            return;
        gl.glDisable( GL2.GL_DEPTH_TEST );
        gl.glDisable( GL2.GL_LINE_SMOOTH );

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
        GL2 gl = glDrawable.getGL().getGL2();

        // Retarded JOGL GLJPanel frequently reallocates the GL context
        // during resize. So we need to be ready to reinitialize everything.
        bBuffersNeedUpload = true;
        shader.unload(gl);
        shader = null;
        dropBuffers(gl);
    }
    
    public void refresh() {
        bBuffersNeedUpload = true;        
    }

    private boolean initializeShaderValues(GL2GL3 gl) {
        boolean rtnVal = true;
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
            
            if (configurator.isUseIdAttribute()) {
                idAttributeLoc = gl.glGetAttribLocation(shader.getShaderProgram(), MeshDrawShader.ID_ATTRIBUTE_NAME);
                reportError(gl, "Obtaining the in-shader locations-4.");
            }

        } catch ( AbstractShader.ShaderCreationException sce ) {
            sce.printStackTrace();
            rtnVal = false;
        }
        return rtnVal;
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

    private void setColoring(GL2GL3 gl) throws AbstractShader.ShaderCreationException {
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

        if (reportError( gl, "Set coloring." )) {
            throw new AbstractShader.ShaderCreationException("Failed to set coloring");
        }
    }
    
    protected void dropBuffers(GL2GL3 gl) {
        BufferUploader bufferUploader = configurator.getBufferUploader();
        int vtxAttribBufferHandle = bufferUploader.getVtxAttribBufferHandle();
        int inxBufferHandle = bufferUploader.getInxBufferHandle();
        if (vtxAttribBufferHandle > -1) {
            gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vtxAttribBufferHandle);
            tempBuffer.rewind();
            tempBuffer.put(vtxAttribBufferHandle);
            tempBuffer.rewind();
            gl.glDeleteBuffers(1, tempBuffer);
            gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);
            reportError( gl, "Drop Vertex Buffer");
        }
        if (inxBufferHandle > -1) {
            gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, inxBufferHandle);
            tempBuffer.rewind();
            tempBuffer.put(inxBufferHandle);
            tempBuffer.rewind();
            gl.glDeleteBuffers(1, tempBuffer);
            gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, 0);
            reportError( gl, "Drop Index Buffer");
        }
    }

}
