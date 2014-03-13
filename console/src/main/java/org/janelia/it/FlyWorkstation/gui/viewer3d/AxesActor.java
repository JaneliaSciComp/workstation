package org.janelia.it.FlyWorkstation.gui.viewer3d;

import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.opengl.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.shader.AbstractShader;
import org.janelia.it.FlyWorkstation.gui.viewer3d.shader.AxesShader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Draws three conventional axes, with tick marks for scale.
 *
 * @author fosterl
 *
 */
public class AxesActor implements GLActor {
    private static final String MODEL_VIEW_UNIFORM_NAME = "modelView";
    private static final String PROJECTION_UNIFORM_NAME = "projection";

    private static final double DEFAULT_AXIS_LEN = 1000.0;
    public static final float TICK_SIZE = 15.0f;
    public static final float SCENE_UNITS_BETWEEN_TICKS = 100.0f;

    public enum RenderMethod {MAXIMUM_INTENSITY, ALPHA_BLENDING}

    // Vary these parameters to taste
	// Rendering variables
	private RenderMethod renderMethod =
		//RenderMethod.ALPHA_BLENDING;
		RenderMethod.MAXIMUM_INTENSITY; // MIP
    private boolean bIsInitialized = false;
    private boolean bFullAxes = false;
    private double axisLengthDivisor = 1.0;

    // OpenGL state
    private boolean bBuffersNeedUpload = true;
    private double[] axisLengths = new double[ 3 ];

    private int lineBufferHandle;
    private int inxBufferHandle;

    private int lineBufferVertexCount = 0;
    private int vertexAttributeLoc = -1;
    private AxesShader shader;
    private IntBuffer tempBuffer = IntBuffer.allocate(1);
    private VolumeModel volumeModel;

    private static Logger logger = LoggerFactory.getLogger( AxesActor.class );

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

    AxesActor() {
        setAxisLengths( DEFAULT_AXIS_LEN, DEFAULT_AXIS_LEN, DEFAULT_AXIS_LEN );
    }

    public void setAxisLengths( double xAxisLength, double yAxisLength, double zAxisLength ) {
        axisLengths[ 0 ] = xAxisLength;
        axisLengths[ 1 ] = yAxisLength;
        axisLengths[ 2 ] = zAxisLength;
        logger.trace("Axial lengths are {}, {} and " + zAxisLength, xAxisLength, yAxisLength);
    }

    public void setAxisLengthDivisor( double axisLengthDivisor ) {
        if ( axisLengthDivisor != 0.0 ) {
            this.axisLengthDivisor = axisLengthDivisor;
        }
    }

    public void setVolumeModel( VolumeModel volumeModel ) {
        this.volumeModel = volumeModel;
    }

    public boolean isFullAxes() {
        return bFullAxes;
    }

    public void setFullAxes( boolean fullAxes ) {
        this.bFullAxes = fullAxes;
    }

    //---------------------------------------IMPLEMEMNTS GLActor
    @Override
	public void init(GLAutoDrawable glDrawable) {
        GL2GL3 gl = glDrawable.getGL().getGL2GL3();

        if (bBuffersNeedUpload) {
            try {
                // Uploading buffers sufficient to draw the axes, ticks, etc.
                initializeShaderValues(gl);
                buildBuffers(gl);

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
        reportError( gl, "Display of axes-actor upon entry" );

        gl.glDisable(GL2GL3.GL_CULL_FACE);
        gl.glFrontFace(GL2GL3.GL_CW);
        reportError( gl, "Display of axes-actor cull-face" );

        // set blending to enable transparent voxels
        gl.glEnable(GL2GL3.GL_BLEND);
        if (renderMethod == RenderMethod.ALPHA_BLENDING) {
            gl.glBlendEquation(GL2GL3.GL_FUNC_ADD);
            // Weight source by GL_ONE because we are using premultiplied alpha.
            gl.glBlendFunc(GL2GL3.GL_ONE, GL2GL3.GL_ONE_MINUS_SRC_ALPHA);
            reportError( gl, "Display of axes-actor alpha" );
        }
        else if (renderMethod == RenderMethod.MAXIMUM_INTENSITY) {
            gl.glBlendEquation(GL2GL3.GL_MAX);
            gl.glBlendFunc(GL2GL3.GL_ONE, GL2GL3.GL_DST_ALPHA);
            reportError( gl, "Display of axes-actor maxintensity" );
        }

        // Draw the little lines.
        tempBuffer.rewind();
        gl.glGetIntegerv(GL2.GL_CURRENT_PROGRAM, tempBuffer);
        int oldProgram = tempBuffer.get();

        gl.glUseProgram( shader.getShaderProgram() );
        gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, lineBufferHandle);
        reportError( gl, "Display of axes-actor 1" );

        shader.setUniformMatrix4v( gl, PROJECTION_UNIFORM_NAME, false, volumeModel.getPerspectiveMatrix() );
        shader.setUniformMatrix4v( gl, MODEL_VIEW_UNIFORM_NAME, false, volumeModel.getModelViewMatrix() );

        // 3 floats per coord. Stride is 0, offset to first is 0.
        gl.glEnableVertexAttribArray(vertexAttributeLoc);
        gl.glVertexAttribPointer(vertexAttributeLoc, 3, GL2.GL_FLOAT, false, 0, 0);
        reportError( gl, "Display of axes-actor 2" );

        gl.glBindBuffer( GL2.GL_ELEMENT_ARRAY_BUFFER, inxBufferHandle );
        reportError(gl, "Display of axes-actor 3.");

        setColoring( gl );

        gl.glDrawElements( GL2.GL_LINES, lineBufferVertexCount, GL2.GL_UNSIGNED_INT, 0 );
        reportError( gl, "Display of axes-actor 4" );

        gl.glUseProgram( oldProgram );

        gl.glDisable(GL2.GL_BLEND);
        reportError(gl, "Axes-actor, end of display.");

	}

    @Override
	public void dispose(GLAutoDrawable glDrawable) {
        GL2 gl = glDrawable.getGL().getGL2();

		// Retarded JOGL GLJPanel frequently reallocates the GL context
		// during resize. So we need to be ready to reinitialize everything.
        bIsInitialized = false;

        IntBuffer toRelease = IntBuffer.allocate(1);
        toRelease.put( lineBufferHandle );
        toRelease.rewind();
        gl.glDeleteBuffers( 1,  toRelease );
        bBuffersNeedUpload = true;
	}

    @Override
	public BoundingBox3d getBoundingBox3d() {
		BoundingBox3d result = new BoundingBox3d();
		Vec3 half = new Vec3(0,0,0);
        for (int i = 0; i < 3; ++i)
            half.set(i, 0.5 * axisLengths[i]);
        result.include(half.minus());
		result.include(half);
		return result;
	}
    //---------------------------------------END IMPLEMENTATION GLActor

    /** Call this when this actor is to be re-shown after an absense. */
    public void refresh() {
    }

    private void initializeShaderValues(GL2GL3 gl) {
        try {
            shader = new AxesShader();
            shader.load( gl.getGL2() );

            vertexAttributeLoc = gl.glGetAttribLocation(shader.getShaderProgram(), AxesShader.VERTEX_ATTRIBUTE_NAME);
            reportError( gl, "Obtaining the in-shader locations." );
            setColoring(gl);


        } catch ( AbstractShader.ShaderCreationException sce ) {
            sce.printStackTrace();
            throw new RuntimeException( sce );
        }

    }

    private void setColoring(GL2GL3 gl) {
        // Must upload the color value for display, at init time.
        float grayValue = 0.15f;
        boolean wasSet = shader.setUniform4v(gl, AxesShader.COLOR_UNIFORM_NAME, 1, new float[]{
                grayValue * 2.0f, grayValue, grayValue, 1.0f
        });
        if ( ! wasSet ) {
            logger.error("Failed to set the " + AxesShader.COLOR_UNIFORM_NAME + " to desired value.");
        }

        reportError( gl, "Set coloring." );
    }

    private void buildBuffers(GL2GL3 gl) {
        BoundingBox3d boundingBox = getBoundingBox3d();
        int nextVertexOffset = 0;
        Geometry axisGeometry = getAxisGeometry(boundingBox, nextVertexOffset);
        nextVertexOffset += axisGeometry.getVertexCount();

        Geometry xShapeGeometry = getXShapeGeometry(boundingBox, nextVertexOffset);
        nextVertexOffset += xShapeGeometry.getVertexCount();

        Geometry yShapeGeometry = getYShapeGeometry(boundingBox, nextVertexOffset);
        nextVertexOffset += yShapeGeometry.getVertexCount();

        Geometry zShapeGeometry = getZShapeGeometry(boundingBox, nextVertexOffset);
        nextVertexOffset += zShapeGeometry.getVertexCount();

        float[] tickOrigin = new float[] {
                (float)boundingBox.getMinX(),
                (float)boundingBox.getMaxY(),
                (float)boundingBox.getMaxZ()
        };

        Geometry xTicks = getTickGeometry(tickOrigin, TICK_SIZE, new AxisIteration(0, 1), new AxisIteration(1, -1), 2, nextVertexOffset);
        nextVertexOffset += xTicks.getVertexCount();
        Geometry yTicks = getTickGeometry( tickOrigin, TICK_SIZE, new AxisIteration( 1, -1 ), new AxisIteration( 2, -1 ), 0, nextVertexOffset );
        nextVertexOffset += yTicks.getVertexCount();
        Geometry zTicks = getTickGeometry( tickOrigin, TICK_SIZE, new AxisIteration( 2, -1 ), new AxisIteration( 0, 1 ), 1, nextVertexOffset );
        nextVertexOffset += zTicks.getVertexCount();

        ByteBuffer baseBuffer = ByteBuffer.allocateDirect(
                Float.SIZE / 8 * (
                        axisGeometry.getCoords().length +
                                xShapeGeometry.getCoords().length +
                                yShapeGeometry.getCoords().length +
                                zShapeGeometry.getCoords().length +
                                xTicks.getCoords().length +
                                yTicks.getCoords().length +
                                zTicks.getCoords().length
                )
        );

        baseBuffer.order( ByteOrder.nativeOrder() );
        FloatBuffer lineBuffer = baseBuffer.asFloatBuffer();
        lineBuffer.put( axisGeometry.getCoords() );
        lineBuffer.put( xShapeGeometry.getCoords() );
        lineBuffer.put( yShapeGeometry.getCoords() );
        lineBuffer.put( zShapeGeometry.getCoords() );
        lineBuffer.put(xTicks.getCoords());
        lineBuffer.put(yTicks.getCoords());
        lineBuffer.put(zTicks.getCoords());
        lineBufferVertexCount = lineBuffer.capacity();
        lineBuffer.rewind();

        ByteBuffer inxBase = ByteBuffer.allocateDirect(
                nextVertexOffset * Integer.SIZE / 8 *
                        axisGeometry.getIndices().length +
                        xShapeGeometry.getIndices().length +
                        yShapeGeometry.getIndices().length +
                        zShapeGeometry.getIndices().length +
                        xTicks.getIndices().length +
                        yTicks.getIndices().length +
                        zTicks.getIndices().length

        );

        inxBase.order( ByteOrder.nativeOrder() );
        IntBuffer inxBuf = inxBase.asIntBuffer();
        inxBuf.put( axisGeometry.getIndices() );
        inxBuf.put( xShapeGeometry.getIndices() );
        inxBuf.put( yShapeGeometry.getIndices() );
        inxBuf.put( zShapeGeometry.getIndices() );
        inxBuf.put( xTicks.getIndices() );
        inxBuf.put( yTicks.getIndices() );
        inxBuf.put( zTicks.getIndices() );
        inxBuf.rewind();

        if ( logger.isDebugEnabled() ) {
            for ( int i = 0; i < lineBuffer.capacity(); i++ ) {
                System.out.println("Line buffer " + i + " = " + lineBuffer.get());
            }
            lineBuffer.rewind();

            for ( int i = 0; i < inxBuf.capacity(); i++ ) {
                System.out.println("Index buffer " + i + " = " + inxBuf.get());
            }
            inxBuf.rewind();
        }

        // Push the coords over to GPU.
        // Make handles for subsequent use.
        int[] handleArr = new int[ 1 ];
        gl.glGenBuffers( 1, handleArr, 0 );
        lineBufferHandle = handleArr[ 0 ];

        gl.glGenBuffers( 1, handleArr, 0 );
        inxBufferHandle = handleArr[ 0 ];

        // Bind data to the handle, and upload it to the GPU.
        gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, lineBufferHandle);
        reportError( gl, "Bind buffer" );
        gl.glBufferData(
                GL2GL3.GL_ARRAY_BUFFER,
                (long) (lineBuffer.capacity() * (Float.SIZE / 8)),
                lineBuffer,
                GL2GL3.GL_STATIC_DRAW
        );
        reportError( gl, "Buffer Data" );

        gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, inxBufferHandle );
        reportError(gl, "Bind Inx Buf");

        gl.glBufferData(
                GL2GL3.GL_ELEMENT_ARRAY_BUFFER,
                (long)(inxBuf.capacity() * (Integer.SIZE / 8)),
                inxBuf,
                GL2GL3.GL_STATIC_DRAW
        );
    }

    private Geometry getXShapeGeometry(BoundingBox3d boundingBox, int startingIndex) {
        float[] vertices = getXShapeCoords(
                    boundingBox.getMinX() - getOverhang( boundingBox ) - 2.0f,
                    boundingBox.getMaxY(),
                    boundingBox.getMaxZ()
        );

        int[] indices = getXIndices( startingIndex );
        return new Geometry( vertices, indices );
    }

    private Geometry getYShapeGeometry(BoundingBox3d boundingBox, int startingIndex) {
        float[] vertices = getYShapeCoords(
                boundingBox.getMinX(),
                boundingBox.getMaxY() + getOverhang( boundingBox ) + 2.0f,
                boundingBox.getMaxZ()
        );
        int[] indices = getYIndices( startingIndex );
        return new Geometry( vertices, indices );
    }

    private Geometry getZShapeGeometry(BoundingBox3d boundingBox, int startingIndex) {
        float[] vertices = getZShapeCoords(
                boundingBox.getMinX(),
                boundingBox.getMaxY(),
                boundingBox.getMaxZ() + getOverhang( boundingBox ) + 2.0f
        );
        int[] indices = getZIndices(startingIndex);
        return new Geometry( vertices, indices );
    }

    private Geometry getAxisGeometry(BoundingBox3d boundingBox, int indexOffset) {
        // Coords includes three line segments, requiring two endpoints, and one for each axis.
        int coordsPerAxis = 3 * 2;
        float[] vertices = new float[ axisLengths.length * coordsPerAxis ];

        // Notes on shape:
        //   Want to have the axes all pass through the origin, but go beyond just a few voxels, to avoid having
        //   lines on a single plane running together too much.
        // Start of X
        float overhang = getOverhang(boundingBox);
        vertices[ 0 ] = (float)boundingBox.getMinX() - overhang;
        vertices[ 1 ] = (float)boundingBox.getMaxY();
        vertices[ 2 ] = (float)boundingBox.getMaxZ();

        // End of X
        vertices[ 3 ] = bFullAxes ? (float)boundingBox.getMaxX() : (float)boundingBox.getMinX() + 100;
        vertices[ 4 ] = (float)boundingBox.getMaxY();
        vertices[ 5 ] = (float)boundingBox.getMaxZ();

        // Start of Y
        vertices[ 6 ] = (float)boundingBox.getMinX();
        vertices[ 7 ] = (float)boundingBox.getMaxY() + overhang;
        vertices[ 8 ] = (float)boundingBox.getMaxZ();

        // End of Y
        vertices[ 9 ] = (float)boundingBox.getMinX();
        vertices[ 10 ] = bFullAxes ? (float)boundingBox.getMinY() : (float)boundingBox.getMaxY() - 100;
        vertices[ 11 ] = (float)boundingBox.getMaxZ();

        // Start of Z
        vertices[ 12 ] = (float)boundingBox.getMinX();
        vertices[ 13 ] = (float)boundingBox.getMaxY();
        vertices[ 14 ] = (float)boundingBox.getMaxZ() + overhang;

        // End of Z
        vertices[ 15 ] = (float)boundingBox.getMinX();
        vertices[ 16 ] = (float)boundingBox.getMaxY();
        vertices[ 17 ] = bFullAxes ? (float)boundingBox.getMinZ() : (float)boundingBox.getMaxZ() - 100;

        int[] indices = new int[ coordsPerAxis ];
        for ( int i = 0; i < 3; i++ ) {
            indices[ 2*i ] = 2*i + indexOffset;
            indices[ 2*i + 1 ] = 2*i + 1 + indexOffset;
        }

        return new Geometry( vertices, indices );
    }

    private float getOverhang(BoundingBox3d boundingBox) {
        return (float)boundingBox.getDepth() / 8.0f;
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

    //  This section makes coordinates for specifically-required letters of the alphabet.
    private float[] getXShapeCoords( double xCenter, double yCenter, double zCenter ) {
        float[] rtnVal = new float[ 4 * 3 ];
        // Top-left stroke start.
        rtnVal[ 0 ] = (float)xCenter - 5.0f;
        rtnVal[ 1 ] = (float)yCenter + 6.0f;
        rtnVal[ 2 ] = (float)zCenter + 5.0f;
        // Bottom-right stroke end.
        rtnVal[ 3 ] = (float)xCenter + 5.0f;
        rtnVal[ 4 ] = (float)yCenter - 6.0f;
        rtnVal[ 5 ] = (float)zCenter + 5.0f;

        // Top-right stroke start.
        rtnVal[ 6 ] = (float)xCenter + 5.0f;
        rtnVal[ 7 ] = (float)yCenter + 6.0f;
        rtnVal[ 8 ] = (float)zCenter + 5.0f;
        // Bottom-left stroke end.
        rtnVal[ 9 ] = (float)xCenter - 5.0f;
        rtnVal[ 10 ] = (float)yCenter - 6.0f;
        rtnVal[ 11 ] = (float)zCenter + 5.0f;

        return rtnVal;
    }

    private float[] getYShapeCoords( double xCenter, double yCenter, double zCenter ) {
        // Only four points are needed.  However, the indices need to use one coord twice.
        float[] rtnVal = new float[ 4 * 3 ];
        // Top-left stroke start.
        rtnVal[ 0 ] = (float)xCenter - 5.0f;
        rtnVal[ 1 ] = (float)yCenter - 6.0f;
        rtnVal[ 2 ] = (float)zCenter + 5.0f;
        // Center stroke end.
        rtnVal[ 3 ] = (float)xCenter;
        rtnVal[ 4 ] = (float)yCenter;
        rtnVal[ 5 ] = (float)zCenter + 5.0f;

        // Top-right stroke start.
        rtnVal[ 6 ] = (float)xCenter + 5.0f;
        rtnVal[ 7 ] = (float)yCenter - 6.0f;
        rtnVal[ 8 ] = (float)zCenter + 5.0f;
        // Top-right stroke ends at Center stroke end.

        // Bottom-stroke end.
        rtnVal[ 9 ] = (float)xCenter;
        rtnVal[ 10 ] = (float)yCenter + 6.0f;
        rtnVal[ 11 ] = (float)zCenter + 5.0f;

        return rtnVal;
    }

    private float[] getZShapeCoords( double xCenter, double yCenter, double zCenter ) {
        float[] rtnVal = new float[ 4 * 3 ];
        // Top-left stroke start.
        //0
        rtnVal[ 0 ] = (float)xCenter - 5.0f;
        rtnVal[ 1 ] = (float)yCenter - 6.0f;
        rtnVal[ 2 ] = (float)zCenter + 5.0f;
        // Top-right stroke end.
        //1
        rtnVal[ 3 ] = (float)xCenter + 5.0f;
        rtnVal[ 4 ] = (float)yCenter - 6.0f;
        rtnVal[ 5 ] = (float)zCenter + 5.0f;

        // Bottom-left stroke start.
        //2
        rtnVal[ 6 ] = (float)xCenter - 5.0f;
        rtnVal[ 7 ] = (float)yCenter + 6.0f;
        rtnVal[ 8 ] = (float)zCenter + 5.0f;
        // Bottom-right stroke end.
        //3
        rtnVal[ 9 ] = (float)xCenter + 5.0f;
        rtnVal[ 10 ] = (float)yCenter + 6.0f;
        rtnVal[ 11 ] = (float)zCenter + 5.0f;

        return rtnVal;
    }

    // Here, the alpahbet letter shape coords are linked using vertices.
    private int[] getXIndices( int offset ) {
        return new int[] {
                0 + offset, 1 + offset, 2 + offset, 3 + offset
        };
    }

    private int[] getYIndices( int offset ) {
        return new int[] {
                0 + offset, 1 + offset, 2 + offset, 1 + offset, 1 + offset, 3 + offset
        };
    }

    private int[] getZIndices( int offset ) {
        return new int[] {
                0 + offset, 1 + offset, 0 + offset, 3 + offset, 2 + offset, 3 + offset
        };
    }

    // Tick mark support.
    private int getTickCount( int axisLength ) {
        // Going for one / 100
        return (int)(axisLength / SCENE_UNITS_BETWEEN_TICKS * axisLengthDivisor) + 1;
    }

    /**
     * Ticks have all of a certain coordinate of one axis (the constant axis) the same.  They have an axis along
     * which they progress (tick 1, tick 2, ... etc., occur along the tick axis).  They have a variance axis: that
     * is the tick's line segment grows in one particular direction (the tick shape axis).  All are established
     * relative to some origin.  Ticks move between one vertex and the other over a certain distance (the tick size).
     *
     * @param origin all vertices are relative to this.
     * @param tickSize how big will the ticks be?
     * @param tickAxis along which axis will ticks be placed?
     * @param tickShapeAxis when a tick is drawn, which way?
     * @param constantAxis this one stays same as that of origin, for all tick vertices.
     * @param baseInxOffset starting index.  Will use this as first shape index, and subsequent relative to it.
     * @return geometry containing both vertices and the line indices.
     */
    private Geometry getTickGeometry(
            float[] origin, float tickSize,
            AxisIteration tickAxis,
            AxisIteration tickShapeAxisIteration,
            int constantAxis,
            int baseInxOffset
    ) {
        int tickCount = getTickCount(new Float(axisLengths[tickAxis.getAxisNum()]).intValue());
        if ( tickCount == 0 ) tickCount = 2;
        int tickOffsDiv = ( axisLengthDivisor == 0 ) ? 1 : (int)axisLengthDivisor;
        int tickOffset = (int) SCENE_UNITS_BETWEEN_TICKS / tickOffsDiv; //(int)axisLengths[ tickAxis.getAxisNum() ] / tickCount;
        float[] vertices = new float[ tickCount * 6 ];
        int[] indices = new int[ 2 * tickCount ];

        int indexCount = 0;
        for ( int i = 0; i < tickCount; i++ ) {
            // Drawing path along one axis.
            float axisOffset = origin[ tickAxis.getAxisNum() ] + (tickAxis.getIterationDirectionMultiplier() * (float)(i * tickOffset) );
            for ( int vertexI = 0; vertexI < 2; vertexI++ ) {
                float tickVariance = origin[ tickShapeAxisIteration.getAxisNum() ] +
                        ( tickShapeAxisIteration.getIterationDirectionMultiplier() * ( vertexI * tickSize ) );
                vertices[ i * 6 + vertexI * 3 + tickAxis.getAxisNum() ] = axisOffset;
                vertices[ i * 6 + vertexI * 3 + tickShapeAxisIteration.getAxisNum() ] = tickVariance;
                vertices[ i * 6 + vertexI * 3 + constantAxis ] = origin[ constantAxis ];

                // The indices of these little lines run n, n+1 for each.
                indices[ indexCount ] = baseInxOffset + (indexCount ++);
            }
        }

        Geometry rtnVal = new Geometry( vertices, indices );
        return rtnVal;
    }

    /**
     * Convenience class to carry around all numbers associated with some thing to draw.
     *
     * A few subtle points about geometry:
     * 1. indices point at vertices.
     * 2. each vertex consists of three coordinates, here: 0th is x, 1st is y, 2nd is z
     * 3. there are always 3x coords as vertices.
     * 4. a count in an index (1,2,3...) corresponds to a by-3 in coords (3,6,9...)
     *
     * Since this class keeps all its data in one buffer for vertices and one for indices, it is
     * important to keep this in mind when incrementing the count.  The value passed in as the next index
     * is actually going to be the next _vertex_ after all vertices of the previous displayable 'shape'.
     */
    private class Geometry {
        private float[] vertices;
        private int[] indices;

        public Geometry( float[] vertices, int[] indices ) {
            this.vertices = vertices;
            this.indices = indices;
        }

        public float[] getCoords() {
            return vertices;
        }

        public int[] getIndices() {
            return indices;
        }

        public int getVertexCount() {
            return getCoords().length / 3;
        }

    }

    private class AxisIteration {
        private int axisNum;
        private int iterationDirectionMultiplier;

        public AxisIteration( int axisNum, int iterationDirectionMultiplier ) {
            this.axisNum = axisNum;
            this.iterationDirectionMultiplier = iterationDirectionMultiplier;
        }

        public int getAxisNum() {
            return axisNum;
        }

        public int getIterationDirectionMultiplier() {
            return iterationDirectionMultiplier;
        }

    }

}
