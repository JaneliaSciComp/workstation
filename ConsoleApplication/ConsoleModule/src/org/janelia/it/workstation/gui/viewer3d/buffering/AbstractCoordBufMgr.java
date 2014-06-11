package org.janelia.it.workstation.gui.viewer3d.buffering;

import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureMediator;
import org.janelia.it.workstation.shared.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL2;
import java.nio.*;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 6/14/13
 * Time: 10:17 AM
 *
 * This delegate/helper handles buffering required for 3D rendering.
 */
public abstract class AbstractCoordBufMgr implements VtxCoordBufMgrI {
    private static final int NUM_BUFFERS_PER_TYPE = 3 * 2; // 3=x,y,z;  2=pos,neg
    protected static final int VERTICES_PER_SLICE = 6;
    protected static final int COORDS_PER_VERTEX = 3;
    protected static final int NUM_AXES = 3;

    protected int vertexAttributeLoc = 0;
    protected int texCoordAttributeLoc = 1;

    protected boolean drawWithElements = true;

    // Buffer objects for setting geometry on the GPU side.
    //   I need one per starting direction (x,y,z) times one for positive, one for negative.
    //
    protected final FloatBuffer texCoordBuf[] = new FloatBuffer[ NUM_BUFFERS_PER_TYPE ];
    protected final FloatBuffer geometryCoordBuf[] = new FloatBuffer[ NUM_BUFFERS_PER_TYPE ];
    protected final ShortBuffer indexBuf[] = new ShortBuffer[ NUM_BUFFERS_PER_TYPE ];

    protected int[] vertexCountByAxis;

    // Int pointers to use as handles when dealing with GPU.
    protected int[] geometryVertexBufferHandles;
    protected int[] textureCoordBufferHandles;
    protected int[] indexBufferHandles;

    protected TextureMediator textureMediator;

    private final Logger logger = LoggerFactory.getLogger(AbstractCoordBufMgr.class);

    public AbstractCoordBufMgr() {
    }

    public AbstractCoordBufMgr( TextureMediator textureMediator ) {
        this();
        setTextureMediator( textureMediator );
    }
    
    @Override
    public void setTextureMediator( TextureMediator textureMediator ) {
        this.textureMediator = textureMediator;
    }

    /**
     * This method builds the buffers of vertices for both geometry and texture.  These are calculated similarly,
     * but with different ranges.  There are multiple such buffers of both types, and they are kept in arrays.
     * The arrays are indexed as follows:
     * 1. Offsets [ 0..2 ] are for positive direction.
     * 2. Offsets 0,3 are X; 1,4 are Y and 2,5 are Z.
     */
    @Override
    public abstract void buildBuffers();

    @Override
    public void setCoordAttributeLocations(int vertexAttributeLoc, int texCoordAttributeLoc) {
        this.vertexAttributeLoc = vertexAttributeLoc;
        this.texCoordAttributeLoc = texCoordAttributeLoc;
    }

    /**
     * Draw the contents of the buffer as needed. Call this from another draw method.
     *
     * @param gl an openGL object as provided during draw, init, etc.
     * @param axis an X,Y, or Z
     * @param direction inwards/outwards [-1.0, 1.0]
     */
    @Override
    public void draw( GL2 gl, CoordinateAxis axis, double direction ) {
        gl.glDisable(GL2.GL_CULL_FACE);
        gl.glFrontFace(GL2.GL_CW);

        // Point to the right vertex set.
        logger.debug("Bind Coords: vertex");
        bindCoordsBuffer(gl, axis, geometryVertexBufferHandles, direction);

        // 3 floats per coord. Stride is 0, offset to first is 0.
        gl.glEnableVertexAttribArray(vertexAttributeLoc);
        gl.glVertexAttribPointer(vertexAttributeLoc, 3, GL2.GL_FLOAT, false, 0, 0);

        // Point to the right texture coordinate set.
        logger.debug("Bind Coords: tex coords");
        bindCoordsBuffer(gl, axis, textureCoordBufferHandles, direction);

        // 3 floats per coord. Stride is 0, offset to first is 0.
        gl.glEnableVertexAttribArray(texCoordAttributeLoc);
        gl.glVertexAttribPointer(texCoordAttributeLoc, 3, GL2.GL_FLOAT, false, 0, 0);

        // Point to the right index coordinate set.
        //NO buffer binding for indices at this time. LLF
        // bindCoordsBuffer( gl, axis, indexBufferHandles, direction );
        int err = gl.glGetError();
        if ( err != 0 ) {
            logger.error("GL Error {}.", err);
        }

        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        // Tell GPU to draw triangles (interpret every three vertices as a triangle), starting at pos 0,
        // and expect vertex-count worth of vertices to examine.
        if ( drawWithElements ) {
            logger.debug("Bind for draw");
            bindIndexBuffer( gl, axis, indexBufferHandles, direction );
            logger.debug("Draw Elements");
            gl.glDrawElements( GL2.GL_TRIANGLES, getVertexCount( axis ), GL2.GL_UNSIGNED_SHORT, 0 );
        }
        else {
            gl.glDrawArrays(GL2.GL_TRIANGLES, 0, getVertexCount(axis));
        }

    }

    /** This is used ONLY for non-textured rendering.  Shapes only. */
    @Override
    public void drawNoTex( GL2 gl, CoordinateAxis axis, double direction ) {

        logger.info("Using VBO");
        // Point to the right vertex set.

        gl.glShadeModel(GL2.GL_SMOOTH);
        gl.glEnableClientState( GL2.GL_VERTEX_ARRAY );
        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glFrontFace(GL2.GL_CW);

        // 3 floats per coord.  Stride is 0, offset to first is 0.
        bindCoordsBuffer(gl, axis, geometryVertexBufferHandles, direction);

        gl.glVertexPointer(3, GL2.GL_FLOAT, 0, 0);

        int err = gl.glGetError();
        if ( err != 0 ) {
            logger.error("GL Error {}.", err);
        }

        // Tell GPU to draw triangles (interpret every three vertices as a triangle), starting at pos 0,
        //  and expect vertex-count worth of vertices to examine.
        indexBuf[ convertAxisDirectionToOffset( axis, direction) ].rewind();
        gl.glColor4f(1.0f, 1.0f, 0.5f, 1.0f);
        if ( drawWithElements ) {
            gl.glDrawElements(GL2.GL_TRIANGLES, getVertexCount(axis), GL2.GL_UNSIGNED_SHORT, indexBuf[convertAxisDirectionToOffset(axis, direction)]);
        }
        else {
            gl.glDrawArrays(GL2.GL_TRIANGLES, 0, getVertexCount(axis));
        }
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
    }
    
        /**
     * If statical (no repeated-upload) is used for the vertices, then this may be called to reduce
     * memory.
     */
    @Override
    public void dropBuffers() {
        for ( int i = 0; i < NUM_BUFFERS_PER_TYPE; i++ ) {
            geometryCoordBuf[ i ] = null;
            texCoordBuf[ i ] = null;
            indexBuf[ i ] = null;
        }
    }

    /**
     * To use vertex and coordinate data, it must first be uploaded, and enabled.  Its role must
     * be designated, and pointers need to be saved.
     *
     * @param gl for graphic-oriented operations.
     * @throws Exception thrown by any called code.
     */
    @Override
    public void enableBuffers(GL2 gl) throws Exception {
        geometryVertexBufferHandles = enableBuffersOfType(gl, geometryCoordBuf, GL2.GL_ARRAY_BUFFER);
        textureCoordBufferHandles = enableBuffersOfType(gl, texCoordBuf, GL2.GL_ARRAY_BUFFER);
        if ( drawWithElements ) {
            indexBufferHandles = enableBuffersOfType(gl, indexBuf, GL2.GL_ELEMENT_ARRAY_BUFFER);
        }
    }

    @Override
    public void releaseBuffers(GL2 gl) {
        releaseBuffers(gl, geometryVertexBufferHandles);
        geometryVertexBufferHandles = null;
        releaseBuffers(gl, textureCoordBufferHandles);
        textureCoordBufferHandles = null;
        releaseBuffers(gl, indexBufferHandles);
        indexBufferHandles = null;
    }

    /** Convenience method to cut down on repeated code. */
    @NotThreadSafe(why="glBufferData uses glBindBuffer result as state, and should only be called from GL thread")
    private int[] enableBuffersOfType(GL2 gl, Buffer[] buffers, int type ) {
        // Make handles for subsequent use.
        int[] rtnVal = new int[ NUM_BUFFERS_PER_TYPE ];
        gl.glGenBuffers( NUM_BUFFERS_PER_TYPE, rtnVal, 0 );

        // Bind data to the handles, and upload it to the GPU.
        for ( int i = 0; i < NUM_BUFFERS_PER_TYPE; i++ ) {
            buffers[ i ].rewind();
            gl.glBindBuffer(type, rtnVal[ i ]);
            // NOTE: this operates on the most recent "bind".  Therefore unless
            // synchronization is in use, this makes the method un-thread-safe.
            // Imagine multiple "bind" and "buffer-data" calls in parallel threads...disaster!
            gl.glBufferData(
                    type,
                    (long)(buffers[ i ].capacity() * (Float.SIZE/8)),
                    buffers[ i ],
                    GL2.GL_STATIC_DRAW
            );

        }

        if ( logger.isDebugEnabled() ) {
            dumpBuffer( buffers );
        }
        return rtnVal;
    }

    private void releaseBuffers( GL2 gl, int[] bufferHandles ) {
        if ( bufferHandles != null ) {
            gl.glDeleteBuffers( bufferHandles.length, bufferHandles, 0 );
        }
    }

    private void dumpBuffer( Buffer[] buffers ) {
        System.out.println("DUMPING THE BUFFERS");

        for ( int i = 0; i < buffers.length; i++ ) {
            String label = ((i < 3) ? " +1.0 " : " -1.0 ") + ("XYZ".charAt( i%3 ));
            System.out.println("BUFFER " + label);
            buffers[ i ].rewind();
            for (int j = 0; j < 180; j++) {
                if ( j % 18 == 0 )
                    System.out.print("  SHEET: " + j/18);
                if ( j % 3 == 0 ) {
                    System.out.print(" [" );
                }
                else if ( j > 0 ) {
                    System.out.print(",");
                }
                if ( buffers[ i ] instanceof FloatBuffer ) {
                    float f = ((FloatBuffer)buffers[i]).get();
                    System.out.print(f);
                }
                else if ( buffers[ i ] instanceof ShortBuffer ) {
                    short f = ((ShortBuffer)buffers[i]).get();
                    System.out.print(f);
                }
                if ( j % 3 == 2 ) {
                    System.out.print( "]" );
                }
                if ( j % 18 == 17 ) {
                    System.out.print( "\n" );
                }
            }
            System.out.println();
        }

    }

    /**
     * Here the buffer is actually used, to establish the vertices and texture coordinates for the display.
     *
     * @param axis tells the primary axis.
     * @param direction for positive/negative view perspective.
     */
    private int bindCoordsBuffer(GL2 gl, CoordinateAxis axis, int[] handles, double direction) {
        return bindBuffer(gl, axis, handles, direction, GL2.GL_ARRAY_BUFFER);
    }

    /**
     * Here the buffer is actually used, to establish the indices for drawing.
     *
     * @param axis tells the primary axis.
     * @param direction for positive/negative view perspective.
     */
    private int bindIndexBuffer( GL2 gl, CoordinateAxis axis, int[] handles, double direction ) {
        return bindBuffer(gl, axis, handles, direction, GL2.GL_ELEMENT_ARRAY_BUFFER);
    }

    /** Reduced code redundancy. ALl types of buffers bound here. */
    private int bindBuffer(GL2 gl, CoordinateAxis axis, int[] handles, double direction, int bufferType ) {
        int bufferOffset = convertAxisDirectionToOffset(axis, direction);
        gl.glBindBuffer(bufferType, handles[bufferOffset]);
        logger.debug("Returning buffer offset of {} for {}.", bufferOffset, axis.getName() + ":" + direction);
        logger.debug("Buffer handle is {}.", handles[bufferOffset]);
        return handles[ bufferOffset ];
    }

    private int convertAxisDirectionToOffset(CoordinateAxis axis, double direction) {
        int directionOffset = 0;
        if ( axis == null  ||  (direction != 1.0  &&  direction != -1.0) ) {
            logger.error("Failed to bind buffer for {}, {}.", axis == null ? "null" : axis.getName(), direction);
            return -1;
        }
        else {
            // Change direction from -1.0 or 1.0 to 0 or 3 as offset into arrays.
            if ( direction == -1.0 ) {
                directionOffset = 3;
            }

        }

        return directionOffset + axis.index();
    }

    private int getVertexCount( CoordinateAxis axis ) {
        return vertexCountByAxis[ axis.index() ];
    }

    protected int computeEffectiveAxisLen(int index) {
        double firstAxisLen = textureMediator.getVolumeMicrometers()[ index % 3 ];
        return (int)(0.5 + firstAxisLen / textureMediator.getVoxelMicrometers()[ index % 3 ]);
    }

    protected void addIndices(int index, short inxOffset) {
        // Indices:  making six vertices from four definitions.
        indexBuf[ index ].put(inxOffset);
        indexBuf[ index ].put((short) (inxOffset + 1));
        indexBuf[ index ].put((short) (inxOffset + 2));

        indexBuf[ index ].put((short) (inxOffset + 1));
        indexBuf[ index ].put((short) (inxOffset + 3));
        indexBuf[ index ].put((short) (inxOffset + 2));
    }

    protected void addTextureCoords(int index, float[] t00, float[] t01, float[] t10, float[] t11) {
        // Triangle 1
        texCoordBuf[ index ].put(t00);
        texCoordBuf[ index ].put(t10);
        texCoordBuf[ index ].put(t01);
        // Triangle 2
        texCoordBuf[ index ].put(t11);
    }

    protected void addGeometry(int index, float[] p00p, float[] p10p, float[] p11p, float[] p01p) {
        // Only need four definitions.
        // Triangle 1
        geometryCoordBuf[ index ].put(p00p);
        geometryCoordBuf[ index ].put(p10p);
        geometryCoordBuf[ index ].put(p01p);
        // Triangle 2
        geometryCoordBuf[ index ].put(p11p);
    }

}
