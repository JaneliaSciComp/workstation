package org.janelia.it.workstation.gui.large_volume_viewer.skeleton;

import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.gui.viewer3d.MeshViewContext;
import org.janelia.it.workstation.gui.viewer3d.text.AxisLabel;
import org.janelia.it.workstation.gui.viewer3d.text.FontInfo;
import static org.janelia.it.workstation.gui.viewer3d.OpenGLUtils.*;
import org.janelia.it.workstation.gui.viewer3d.axes.AxisIteration;
import org.janelia.it.workstation.gui.viewer3d.axes.Geometry;
import org.janelia.it.workstation.gui.viewer3d.matrix_support.MatrixManager;
import org.janelia.it.workstation.gui.viewer3d.shader.AbstractShader;
import org.janelia.it.workstation.gui.viewer3d.text.AxisLabel.AxisOfParallel;

/**
 * Draws three conventional axes, with tick marks for scale.
 *
 * @author fosterl
 *
 */
public class AxesActor implements GLActor
{
    private static final double DEFAULT_AXIS_LEN = 1000.0;
    private static final float DEFAULT_AXIS_LABEL_MULTIPLIER = 1.0f;
    public static final float SCENE_UNITS_BETWEEN_TICKS = 100.0f;

    public enum RenderMethod {MAXIMUM_INTENSITY, ALPHA_BLENDING, MESH}

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
    private BoundingBox3d boundingBox;
    Collection<AxisLabel> labels = new ArrayList<>();
    final int[] handleArr = new int[ 1 ];

    private int lineBufferHandle;
    private int inxBufferHandle;
	private int colorBufferHandle;
    private int interleavedTexHandle;
    
    private FontInfo axisLabelFontInfo;    

    private int lineBufferVertexCount = 0;
    private float sceneUnitsBetweenTicks = SCENE_UNITS_BETWEEN_TICKS;
    private double log10LeastAxis = 2;
    private float axisLabelMagnifier;
    private int tickSize;
    
    private GenericVPHelper vertexPointerHelper;
    private DirectionalReferenceAxesShader shader;
    private MeshViewContext context;
    private MatrixManager matrixManager;

    private static final Logger logger = LoggerFactory.getLogger( AxesActor.class );

    public AxesActor() {
        setAxisLengths( DEFAULT_AXIS_LEN, DEFAULT_AXIS_LEN, DEFAULT_AXIS_LEN );
    }        

    public void setMeshViewerContext( MeshViewContext context ) {
        this.context = context;
    }
    
    /**
     * @return the renderMethod
     */
    public RenderMethod getRenderMethod() {
        return renderMethod;
    }

    /**
     * @param renderMethod the renderMethod to set
     */
    public void setRenderMethod(RenderMethod renderMethod) {
        this.renderMethod = renderMethod;
    }

    public final void setAxisLengths( double xAxisLength, double yAxisLength, double zAxisLength ) {
        axisLengths[ 0 ] = xAxisLength;
        axisLengths[ 1 ] = yAxisLength;
        axisLengths[ 2 ] = zAxisLength;
        double leastAxis = axisLengths[0];
        for ( int i = 1; i < 3; i++ ) {
            if (axisLengths[i] < leastAxis) {
                leastAxis = axisLengths[i];
            }
        }
        log10LeastAxis = Math.floor( Math.max(Math.log10(leastAxis), 1.0) );        
        sceneUnitsBetweenTicks = (float)Math.floor( Math.pow( 10.0, log10LeastAxis ) );
        tickSize = (int)(0.15 * sceneUnitsBetweenTicks);
        if ( log10LeastAxis >= 3.0 ) {
            axisLabelMagnifier = (float)(Math.pow(10.0, log10LeastAxis - 2));
        }
        else {
            axisLabelMagnifier = DEFAULT_AXIS_LABEL_MULTIPLIER;
        }

        logger.trace("Axial lengths are {}, {} and " + zAxisLength, xAxisLength, yAxisLength);
    }
    
    public void setBoundingBox( BoundingBox3d boundingBox ) {
        this.boundingBox = boundingBox;
    }

    public void setAxisLengthDivisor( double axisLengthDivisor ) {
        if ( axisLengthDivisor != 0.0 ) {
            this.axisLengthDivisor = axisLengthDivisor;
        }
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
        GL2GL3 gl = glDrawable.getGL().getGL2();

        if (bBuffersNeedUpload) {            
            try {
                shaderInitialize(gl);
                
                axisLabelFontInfo = new FontInfo(
                        "monospaced", FontInfo.FontStyle.Plain, 15, "axistickbkbgrnd"
                );
        
                // Uploading buffers sufficient to draw the axes, ticks, etc.
                if (buildBuffers(gl)) {
                    bBuffersNeedUpload = false;
                }
                vertexPointerHelper = new GenericVPHelper(context, "ticked-axes-actor");
                this.matrixManager = new MatrixManager(
                        context, 
                        glDrawable.getWidth(), 
                        glDrawable.getHeight()
                );

            } catch ( Exception ex ) {
                SessionMgr.getSessionMgr().handleException( ex );
            }
        }

		// tidy up
		bIsInitialized = true;
	}

    @Override
	public void display(GLAutoDrawable glDrawable) {
        // May have been instructed to hide these things.
        if ( ! context.isShowAxes() )
            return;
        
        vertexPointerHelper.display(
                glDrawable, 
                shader,
                matrixManager,
                lineBufferHandle,
                colorBufferHandle,
                inxBufferHandle,
                lineBufferVertexCount
        );
	}

    @Override
	public void dispose(GLAutoDrawable glDrawable) {
        GL2GL3 gl = glDrawable.getGL().getGL2();

		// Retarded JOGL GLJPanel frequently reallocates the GL context
		// during resize. So we need to be ready to reinitialize everything.
        bIsInitialized = false;

        IntBuffer toRelease = IntBuffer.allocate(4);
        toRelease.put( lineBufferHandle );
        toRelease.put( interleavedTexHandle );
        toRelease.put( colorBufferHandle );
        toRelease.put( inxBufferHandle );
        toRelease.rewind();
        gl.glDeleteBuffers( 4,  toRelease );
        bBuffersNeedUpload = true;
	}

    @Override
	public BoundingBox3d getBoundingBox3d() {
        BoundingBox3d result;
        if (this.boundingBox == null) {
            result = new BoundingBox3d();
            Vec3 half = new Vec3(0, 0, 0);
            for (int i = 0; i < 3; ++i) {
                half.set(i, 0.5 * axisLengths[i]);
            }
            result.include(half.minus());
            result.include(half);
        }
        else {
            result = this.boundingBox;
        }
		return result;
	}
    
    //---------------------------------------END IMPLEMENTATION GLActor

    /** Call this when this actor is to be re-shown after an absence. */
    public void refresh() {
    }

    private void shaderInitialize(GL2GL3 gl) throws AbstractShader.ShaderCreationException {
        shader = new DirectionalReferenceAxesShader();
        shader.init(gl.getGL2());
        reportError(gl.getGL2(), "Initializing shader.");
    }

    private boolean buildBuffers(GL2GL3 gl) {
        boolean rtnVal = true; // True -> successful.
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
        
        Geometry xTicks = getTickGeometry(tickOrigin, tickSize, new AxisIteration(0, 1), new AxisIteration(1, -1), 2, nextVertexOffset);
        nextVertexOffset += xTicks.getVertexCount();
        Geometry yTicks = getTickGeometry( tickOrigin, tickSize, new AxisIteration( 1, -1 ), new AxisIteration( 2, -1 ), 0, nextVertexOffset );
        nextVertexOffset += yTicks.getVertexCount();
        Geometry zTicks = getTickGeometry( tickOrigin, tickSize, new AxisIteration( 2, -1 ), new AxisIteration( 0, 1 ), 1, nextVertexOffset );
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
		
		final int numVertices = baseBuffer.capacity() / 3;		
		final int colorBufferByteSize = (numVertices * 4);
		
		ByteBuffer colorBuffer = ByteBuffer.allocateDirect( colorBufferByteSize );
		colorBuffer.order( ByteOrder.nativeOrder() );
		byte alpha = (byte)255;
		byte[] color = new byte[3];
		if (context.isWhiteBackground()) {
			color[ 0] = (byte)255;
			color[ 1] = (byte)218;
			color[ 2] = (byte)218;
		} else {
			color[ 0] = 77;
			color[ 1] = 38;
			color[ 2] = 38;
		}
		for (int i = 0; i < numVertices; i++) {
			colorBuffer.put(color);
			colorBuffer.put(alpha);
		}
		colorBuffer.rewind();
		//NEXT: push this buffer.  Figure out how to turn it on at display
		
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
        lineBufferHandle = createBufferHandle( gl );
        inxBufferHandle = createBufferHandle( gl );
		colorBufferHandle = createBufferHandle( gl );

        // Bind data to the handle, and upload it to the GPU.
        gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, lineBufferHandle);
        if (reportError( gl, "Bind buffer" )) {
            return false;
        }
        gl.glBufferData(
                GL2GL3.GL_ARRAY_BUFFER,
                (long) (lineBuffer.capacity() * FLOAT_BYTE_SIZE),
                lineBuffer,
                GL2GL3.GL_STATIC_DRAW
        );
        if (reportError( gl, "Buffer Data" )) {
            return false;
        }
		
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, colorBufferHandle);
        if (reportError( gl, "Bind Color buffer" )) {
            return false;
        }
        gl.glBufferData(
                GL2GL3.GL_ARRAY_BUFFER,
                (long) (colorBuffer.capacity()),
                colorBuffer,
                GL2GL3.GL_STATIC_DRAW
        );
        if (reportError( gl, "Color Buffer Data" )) {
            return false;
        }

        gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, inxBufferHandle );
        if (reportError(gl, "Bind Inx Buf")) {
            return false;
        }

        gl.glBufferData(
                GL2GL3.GL_ELEMENT_ARRAY_BUFFER,
                (long)(inxBuf.capacity() * (Integer.SIZE / 8)),
                inxBuf,
                GL2GL3.GL_STATIC_DRAW
        );
        if (reportError(gl, "Push buffers")) {
            return false;
        }
        
        return rtnVal;
    }
	public static final int FLOAT_BYTE_SIZE = Float.SIZE / 8;
    
    private int createBufferHandle( GL2GL3 gl ) {
        gl.glGenBuffers( 1, handleArr, 0 );
        return handleArr[ 0 ];
    }
    
    private boolean buildLabelBuffers( GL2GL3 gl ) {
        boolean rtnVal = true; // True -> success.
        FloatBuffer labelBuf = null;
        int totalBufferSizeBytes = 0;
        for ( AxisLabel label: labels ) {
            totalBufferSizeBytes += Float.SIZE / 8 *
                    (label.getVtxCoords().length
                    + label.getTexCoords().length);
        }
        
        ByteBuffer baseBuffer = ByteBuffer.allocateDirect(
                totalBufferSizeBytes
        );
        baseBuffer.order(ByteOrder.nativeOrder());
        labelBuf = baseBuffer.asFloatBuffer();
        labelBuf.rewind();

        for (AxisLabel label : labels) {
            for (int i = 0; i < label.getVtxCoords().length; i++) {
                labelBuf.put(label.getVtxCoords()[ i ]);
                labelBuf.put(label.getTexCoords()[ i ]);
            }
        }
        labelBuf.rewind();
        
        interleavedTexHandle = createBufferHandle(gl);
        // Bind data to the handle, and upload it to the GPU.
        gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, interleavedTexHandle);
        if (reportError( gl, "Interleaved buffer" )) {
            return false;
        }
        gl.glBufferData(
                GL2GL3.GL_ARRAY_BUFFER,
                (long) (labelBuf.capacity() * FLOAT_BYTE_SIZE),
                labelBuf,
                GL2GL3.GL_STATIC_DRAW
        );
        if (reportError( gl, "Interleaved Data" )) {
            return false;
        }
        return rtnVal;
    }
    
    /** Establish a label for the tick. */
    private void createAxisLabel( 
            float x, float y, float z,
            int labelVal, AxisOfParallel axis
    ) {
        final String labelText = ""+labelVal;

        float height = axisLabelFontInfo.getFontHeight();
        float width = axisLabelFontInfo.getWidth( labelText );
        
        // NOTE: all text bounds will be arranged as if along the X axis,
        // but then the final geometry will be adjusted as needed to align
        // to the relevant axis.
        AxisLabel.TextBounds textBounds = new AxisLabel.TextBounds( x - width/2, y, z, width, height );
        AxisLabel axisLabel = new AxisLabel( labelText, axisLabelFontInfo, textBounds, axis );
        
        labels.add( axisLabel );
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

    //  This section makes coordinates for specifically-required letters of the alphabet.
    private float[] getXShapeCoords( double xCenter, double yCenter, double zCenter ) {
        float[] rtnVal = new float[ 4 * 3 ];
        // Top-left stroke start.
        rtnVal[ 0 ] = (float)xCenter - 5.0f * axisLabelMagnifier;
        rtnVal[ 1 ] = (float)yCenter + 6.0f * axisLabelMagnifier;
        rtnVal[ 2 ] = (float)zCenter + 5.0f * axisLabelMagnifier;
        // Bottom-right stroke end.
        rtnVal[ 3 ] = (float)xCenter + 5.0f * axisLabelMagnifier;
        rtnVal[ 4 ] = (float)yCenter - 6.0f * axisLabelMagnifier;
        rtnVal[ 5 ] = (float)zCenter + 5.0f * axisLabelMagnifier;

        // Top-right stroke start.
        rtnVal[ 6 ] = (float)xCenter + 5.0f * axisLabelMagnifier;
        rtnVal[ 7 ] = (float)yCenter + 6.0f * axisLabelMagnifier;
        rtnVal[ 8 ] = (float)zCenter + 5.0f * axisLabelMagnifier;
        // Bottom-left stroke end.
        rtnVal[ 9 ] = (float)xCenter - 5.0f * axisLabelMagnifier;
        rtnVal[ 10 ] = (float)yCenter - 6.0f * axisLabelMagnifier;
        rtnVal[ 11 ] = (float)zCenter + 5.0f * axisLabelMagnifier;

        return rtnVal;
    }

    private float[] getYShapeCoords( double xCenter, double yCenter, double zCenter ) {
        // Only four points are needed.  However, the indices need to use one coord twice.
        float[] rtnVal = new float[ 4 * 3 ];
        // Top-left stroke start.
        rtnVal[ 0 ] = (float)xCenter - 5.0f * axisLabelMagnifier;
        rtnVal[ 1 ] = (float)yCenter - 6.0f * axisLabelMagnifier;
        rtnVal[ 2 ] = (float)zCenter + 5.0f * axisLabelMagnifier;
        // Center stroke end.
        rtnVal[ 3 ] = (float)xCenter;
        rtnVal[ 4 ] = (float)yCenter;
        rtnVal[ 5 ] = (float)zCenter + 5.0f * axisLabelMagnifier;

        // Top-right stroke start.
        rtnVal[ 6 ] = (float)xCenter + 5.0f * axisLabelMagnifier;
        rtnVal[ 7 ] = (float)yCenter - 6.0f * axisLabelMagnifier;
        rtnVal[ 8 ] = (float)zCenter + 5.0f * axisLabelMagnifier;
        // Top-right stroke ends at Center stroke end.

        // Bottom-stroke end.
        rtnVal[ 9 ] = (float)xCenter;
        rtnVal[ 10 ] = (float)yCenter + 6.0f * axisLabelMagnifier;
        rtnVal[ 11 ] = (float)zCenter + 5.0f * axisLabelMagnifier;

        return rtnVal;
    }

    private float[] getZShapeCoords( double xCenter, double yCenter, double zCenter ) {
        float[] rtnVal = new float[ 4 * 3 ];
        // Top-left stroke start.
        //0
        rtnVal[ 0 ] = (float)xCenter - 5.0f * axisLabelMagnifier;
        rtnVal[ 1 ] = (float)yCenter - 6.0f * axisLabelMagnifier;
        rtnVal[ 2 ] = (float)zCenter + 5.0f * axisLabelMagnifier;
        // Top-right stroke end.
        //1
        rtnVal[ 3 ] = (float)xCenter + 5.0f * axisLabelMagnifier;
        rtnVal[ 4 ] = (float)yCenter - 6.0f * axisLabelMagnifier;
        rtnVal[ 5 ] = (float)zCenter + 5.0f * axisLabelMagnifier;

        // Bottom-left stroke start.
        //2
        rtnVal[ 6 ] = (float)xCenter - 5.0f * axisLabelMagnifier;
        rtnVal[ 7 ] = (float)yCenter + 6.0f * axisLabelMagnifier;
        rtnVal[ 8 ] = (float)zCenter + 5.0f * axisLabelMagnifier;
        // Bottom-right stroke end.
        //3
        rtnVal[ 9 ] = (float)xCenter + 5.0f * axisLabelMagnifier;
        rtnVal[ 10 ] = (float)yCenter + 6.0f * axisLabelMagnifier;
        rtnVal[ 11 ] = (float)zCenter + 5.0f * axisLabelMagnifier;

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
        return (int)(axisLength / sceneUnitsBetweenTicks * axisLengthDivisor + 1);
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
        int tickOffset = (int) sceneUnitsBetweenTicks / tickOffsDiv; //(int)axisLengths[ tickAxis.getAxisNum() ] / tickCount;
        float[] vertices = new float[ tickCount * 6 ];
        int[] indices = new int[ 2 * tickCount ];

        int indexCount = 0;        
        for ( int i = 0; i < tickCount; i++ ) {
            // Drawing path along one axis.
            float axisOffset = origin[ tickAxis.getAxisNum() ] + (tickAxis.getIterationDirectionMultiplier() * (float)(i * tickOffset) );
            for ( int vertexI = 0; vertexI < 2; vertexI++ ) {
                float tickVariance = origin[ tickShapeAxisIteration.getAxisNum() ] +
                        ( tickShapeAxisIteration.getIterationDirectionMultiplier() * ( vertexI * tickSize ) );
                final int xOffset = i * 6 + vertexI * 3 + tickAxis.getAxisNum();
                vertices[ xOffset ] = axisOffset;
                final int yOffset = i * 6 + vertexI * 3 + tickShapeAxisIteration.getAxisNum();
                vertices[ yOffset ] = tickVariance;
                final int zOffset = i * 6 + vertexI * 3 + constantAxis;
                vertices[ zOffset ] = origin[ constantAxis ];

                // The indices of these little lines run n, n+1 for each.
                indices[ indexCount ] = baseInxOffset + (indexCount ++);
                
                // Label at tips of all axes.
                if ( vertexI == 1   &&   i > 0 ) {
                    createAxisLabel(                             
                            vertices[ xOffset ], vertices[ yOffset ], vertices[ zOffset ],
                            (tickCount) * 100,
                            AxisOfParallel.getValue( constantAxis )
                    );
                }
            }
        }

        Geometry rtnVal = new Geometry( vertices, indices );
        return rtnVal;
    }

}
