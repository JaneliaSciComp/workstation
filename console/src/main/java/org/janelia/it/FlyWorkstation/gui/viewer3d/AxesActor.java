package org.janelia.it.FlyWorkstation.gui.viewer3d;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL2;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
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
public class AxesActor implements GLActor
{
    private static final double DEFAULT_AXIS_LEN = 1000.0;

    public enum RenderMethod {MAXIMUM_INTENSITY, ALPHA_BLENDING}

    // Vary these parameters to taste
	// Rendering variables
	private RenderMethod renderMethod =
		// RenderMethod.ALPHA_BLENDING;
		RenderMethod.MAXIMUM_INTENSITY; // MIP
    private boolean bIsInitialized = false;
    private boolean bFullAxes = false;

    // OpenGL state
    private boolean bBuffersNeedUpload = true;
    private double[] axisLengths = new double[ 3 ];

    //private FloatBuffer lineBuffer;
    private int lineBufferHandle;
    private int inxBufferHandle;

    private int lineBufferVertexCount = 0;

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
    }

    public boolean isFullAxes() {
        return bFullAxes;
    }

    public void setFullAxes( boolean fullAxes ) {
        this.bFullAxes = fullAxes;
    }

    //---------------------------------------IMPLEMEMNTS GLActor
    @Override
	public void init(GL2 gl) {

        if (bBuffersNeedUpload) {
            try {
                // Uploading buffers sufficient to draw the axes, ticks, etc.
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
	public void display(GL2 gl) {

        gl.glDisable(GL2.GL_CULL_FACE);
        gl.glFrontFace(GL2.GL_CW);

		gl.glPushAttrib(GL2.GL_LIGHTING_BIT | GL2.GL_ENABLE_BIT);
		gl.glShadeModel(GL2.GL_FLAT);
        gl.glDisable(GL2.GL_LIGHTING);

        // set blending to enable transparent voxels
        if (renderMethod == RenderMethod.ALPHA_BLENDING) {
            gl.glEnable(GL2.GL_BLEND);
            gl.glBlendEquation(GL2.GL_FUNC_ADD);
            // Weight source by GL_ONE because we are using premultiplied alpha.
            gl.glBlendFunc(GL2.GL_ONE, GL2.GL_ONE_MINUS_SRC_ALPHA);
        }
        else if (renderMethod == RenderMethod.MAXIMUM_INTENSITY) {
    	    gl.glEnable(GL2.GL_BLEND);
            gl.glBlendEquation(GL2.GL_MAX);
            gl.glBlendFunc(GL2.GL_ONE, GL2.GL_DST_ALPHA);
            // gl.glBlendFunc(GL2.GL_ONE_MINUS_DST_COLOR, GL2.GL_ZERO); // inverted?  http://stackoverflow.com/questions/2656905/opengl-invert-framebuffer-pixels
        }

        gl.glEnable( GL2.GL_LINE_SMOOTH );
        gl.glHint( GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST );

        // Draw the little lines.
        gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, lineBufferHandle );
        reportError( gl, "Display of axes-actor 1" );

        gl.glColor3f( 0.2f, 0.2f, 0.2f );
        reportError( gl, "Display of axes-actor 2" );

        gl.glEnableClientState( GL2.GL_VERTEX_ARRAY );
        reportError( gl, "Display of axes-actor 3" );

        gl.glVertexPointer(3, GL2.GL_FLOAT, 0, 0);
        reportError( gl, "Display of axes-actor 4" );

        gl.glBindBuffer( GL2.GL_ELEMENT_ARRAY_BUFFER, inxBufferHandle );
        reportError(gl, "Display of axes-actor 4a.");

        gl.glDrawElements( GL2.GL_LINES, lineBufferVertexCount, GL2.GL_UNSIGNED_INT, 0 );
        reportError( gl, "Display of axes-actor 5" );

        gl.glDisableClientState( GL2.GL_VERTEX_ARRAY );
        gl.glDisable( GL2.GL_LINE_SMOOTH );
        reportError( gl, "Display of axes-actor 6" );


		gl.glPopAttrib();
	}

    @Override
	public void dispose(GL2 gl) {

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

    private void buildBuffers(GL2 gl) {
        // Coords includes three line segments, requiring two endpoints, and one for each axis.
        int coordsPerAxis = 3 * 2;
        BoundingBox3d boundingBox = getBoundingBox3d();
        float[] coords = new float[ axisLengths.length * coordsPerAxis ];

        // Notes on shape:
        //   Want to have the axes all pass through the origin, but go beyond just a few voxels, to avoid having
        //   lines on a single plane running together too much.

        float overhang = (float)boundingBox.getDepth() / 8.0f;
        // Start of X
        coords[ 0 ] = (float)boundingBox.getMinX() - overhang;
        coords[ 1 ] = (float)boundingBox.getMaxY();
        coords[ 2 ] = (float)boundingBox.getMaxZ();

        // End of X
        coords[ 3 ] = bFullAxes ? (float)boundingBox.getMaxX() : (float)boundingBox.getMinX() + 100;
        coords[ 4 ] = (float)boundingBox.getMaxY();
        coords[ 5 ] = (float)boundingBox.getMaxZ();

        // Start of Y
        coords[ 6 ] = (float)boundingBox.getMinX();
        coords[ 7 ] = (float)boundingBox.getMaxY() + overhang;
        coords[ 8 ] = (float)boundingBox.getMaxZ();

        // End of Y
        coords[ 9 ] = (float)boundingBox.getMinX();
        coords[ 10 ] = bFullAxes ? (float)boundingBox.getMinY() : (float)boundingBox.getMaxY() - 100;
        coords[ 11 ] = (float)boundingBox.getMaxZ();

        // Start of Z
        coords[ 12 ] = (float)boundingBox.getMinX();
        coords[ 13 ] = (float)boundingBox.getMaxY();
        coords[ 14 ] = (float)boundingBox.getMaxZ() + overhang;

        // End of Z
        coords[ 15 ] = (float)boundingBox.getMinX();
        coords[ 16 ] = (float)boundingBox.getMaxY();
        coords[ 17 ] = bFullAxes ? (float)boundingBox.getMinZ() : (float)boundingBox.getMaxZ() - 100;

        float[] xShapeCoords = getXShapeCoords(
                boundingBox.getMinX() - overhang - 2.0f,
                boundingBox.getMaxY(),
                boundingBox.getMaxZ()
        );
        float[] yShapeCoords = getYShapeCoords(
                boundingBox.getMinX(),
                boundingBox.getMaxY() + overhang + 2.0f,
                boundingBox.getMaxZ()
        );
        float[] zShapeCoords = getZShapeCoords(
                boundingBox.getMinX(),
                boundingBox.getMaxY(),
                boundingBox.getMaxZ() + overhang + 2.0f
        );

        ByteBuffer baseBuffer = ByteBuffer.allocateDirect( Float.SIZE / 8 * (coords.length + xShapeCoords.length + yShapeCoords.length + zShapeCoords.length) );
        baseBuffer.order( ByteOrder.nativeOrder() );
        FloatBuffer lineBuffer = baseBuffer.asFloatBuffer();
        lineBuffer.put( coords );
        lineBuffer.put( xShapeCoords );
        lineBuffer.put( yShapeCoords );
        lineBuffer.put( zShapeCoords );
        lineBuffer.rewind();

        int numIndices = 6;
        int numVertices = 6;
        int[] xInx = getXIndices( numIndices );
        numIndices += 4;
        numVertices += xInx.length;
        int[] yInx = getYIndices(numIndices);
        numIndices += 4;
        numVertices += yInx.length;
        int[] zInx = getZIndices( numIndices );
        numIndices += 4;
        numVertices += zInx.length;

        ByteBuffer inxBase = ByteBuffer.allocateDirect( numVertices * Integer.SIZE / 8 );
        lineBufferVertexCount = numVertices;

        inxBase.order( ByteOrder.nativeOrder() );
        IntBuffer inxBuf = inxBase.asIntBuffer();
        for ( int i = 0; i < 3; i++ ) {
            inxBuf.put( 2*i );
            inxBuf.put( 2*i + 1 );
        }
        inxBuf.put( xInx );
        inxBuf.put( yInx );
        inxBuf.put( zInx );
        inxBuf.rewind();

        for ( int i = 0; i < lineBuffer.capacity(); i++ ) {
            System.out.println("Line buffer " + i + " = " + lineBuffer.get());
        }
        lineBuffer.rewind();

        for ( int i = 0; i < inxBuf.capacity(); i++ ) {
            System.out.println("Index buffer " + i + " = " + inxBuf.get());
        }
        inxBuf.rewind();

        // Push the coords over to GPU.
        // Make handles for subsequent use.
        int[] handleArr = new int[ 1 ];
        gl.glGenBuffers( 1, handleArr, 0 );
        lineBufferHandle = handleArr[ 0 ];

        gl.glGenBuffers( 1, handleArr, 0 );
        inxBufferHandle = handleArr[ 0 ];

        // Bind data to the handle, and upload it to the GPU.
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, lineBufferHandle);
        reportError( gl, "Bind buffer" );
        gl.glBufferData(
                GL2.GL_ARRAY_BUFFER,
                (long) (lineBuffer.capacity() * (Float.SIZE / 8)),
                lineBuffer,
                GL2.GL_STATIC_DRAW
        );
        reportError( gl, "Buffer Data" );

        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, inxBufferHandle );
        reportError(gl, "Bind Inx Buf");

        gl.glBufferData(
                GL2.GL_ELEMENT_ARRAY_BUFFER,
                (long)(inxBuf.capacity() * (Integer.SIZE / 8)),
                inxBuf,
                GL2.GL_STATIC_DRAW
        );
    }

    /** DEBUG code to help understand what is happening with vtx or tex points. */
    private void printPoints(String type, double[] p1, double[] p2, double[] p3, double[] p4) {
        System.out.print(type + " [");
        printPoint(p1);
        System.out.print("][");
        printPoint(p2);
        System.out.print("][");
        printPoint(p3);
        System.out.print("][");
        printPoint(p4);
        System.out.println("]");
    }

    private void printPoint(double[] p) {
        System.out.printf("%s, %s, %s", Double.toString(p[0]), Double.toString(p[1]), Double.toString(p[2]));
    }

    private void reportError(GL2 gl, String source) {
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

}
