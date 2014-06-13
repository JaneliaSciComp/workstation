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
public class VtxCoordBufMgr {
    private static final int NUM_BUFFERS_PER_TYPE = 3 * 2; // 3=x,y,z;  2=pos,neg
    private static final int VERTICES_PER_SLICE = 6;
    private static final int COORDS_PER_VERTEX = 3;
    private static final int NUM_AXES = 3;

    private int vertexAttributeLoc = 0;
    private int texCoordAttributeLoc = 1;

    private boolean drawWithElements = true;

    // Buffer objects for setting geometry on the GPU side.
    //   I need one per starting direction (x,y,z) times one for positive, one for negative.
    //
    private final FloatBuffer texCoordBuf[] = new FloatBuffer[ NUM_BUFFERS_PER_TYPE ];
    private final FloatBuffer geometryCoordBuf[] = new FloatBuffer[ NUM_BUFFERS_PER_TYPE ];
    private final ShortBuffer indexBuf[] = new ShortBuffer[ NUM_BUFFERS_PER_TYPE ];

    private int[] vertexCountByAxis;

    // Int pointers to use as handles when dealing with GPU.
    private int[] geometryVertexBufferHandles;
    private int[] textureCoordBufferHandles;
    private int[] indexBufferHandles;

    private TextureMediator textureMediator;

    private final Logger logger = LoggerFactory.getLogger(VtxCoordBufMgr.class);

    public VtxCoordBufMgr() {
    }

    public VtxCoordBufMgr( TextureMediator textureMediator ) {
        this();
        setTextureMediator( textureMediator );
    }

    public void setTextureMediator( TextureMediator textureMediator ) {
        this.textureMediator = textureMediator;
    }

    public void setCoordAttributeLocations( int vertexAttributeLoc, int texCoordAttributeLoc ) {
        this.vertexAttributeLoc = vertexAttributeLoc;
        this.texCoordAttributeLoc = texCoordAttributeLoc;
    }

    /**
     * This method builds the buffers of vertices for both geometry and texture.  These are calculated similarly,
     * but with different ranges.  There are multiple such buffers of both types, and they are kept in arrays.
     * The arrays are indexed as follows:
     * 1. Offsets [ 0..2 ] are for positive direction.
     * 2. Offsets 0,3 are X; 1,4 are Y and 2,5 are Z.
     */
    public void buildBuffers() {
        vertexCountByAxis = new int[ NUM_AXES ];
        if ( texCoordBuf[ 0 ] == null  &&  textureMediator != null ) {
            // Compute sizes, and allocate buffers.
            logger.info("Allocating buffers");
            for ( int i = 0; i < NUM_BUFFERS_PER_TYPE; i++ ) {
                // Three coords per vertex.  Six vertexes per slice.  Times number of slices.
                int numVertices = VERTICES_PER_SLICE * computeEffectiveAxisLen(i % NUM_AXES);
                int numCoords = COORDS_PER_VERTEX * numVertices;
                vertexCountByAxis[ i % NUM_AXES ] = numVertices;

                // NOTE on usage.  Here, 'allocateDirect' is used, because otherwise there would be no backing array.
                //  However, this is not guaranteed by the Java NIO contract.  Hence under some circumstances,
                //  this may not be true.  Wrapping an array did not provide a backing array on my system.
                //  Further, the use of a byte-buffer is required because of the different endian-ness between
                //  Java and the native system.
                ByteBuffer texByteBuffer = ByteBuffer.allocateDirect(numCoords * Float.SIZE / 8);
                texByteBuffer.order( ByteOrder.nativeOrder() );
                texCoordBuf[ i ] = texByteBuffer.asFloatBuffer();

                ByteBuffer geoByteBuffer = ByteBuffer.allocateDirect(numCoords * Float.SIZE / 8);
                geoByteBuffer.order(ByteOrder.nativeOrder());
                geometryCoordBuf[ i ] = geoByteBuffer.asFloatBuffer();

                // One index per vertex.  Not one per coord.  No need for x,y,z.
                if ( drawWithElements ) {
                    ByteBuffer inxByteBuffer = ByteBuffer.allocateDirect(numVertices * Short.SIZE / 8);
                    inxByteBuffer.order(ByteOrder.nativeOrder());
                    indexBuf[ i ] = inxByteBuffer.asShortBuffer();
                }
            }

            // Now produce the vertexes to stuff into all of the buffers.
            //  Making sets of four.
            for ( int firstInx = 0; firstInx < NUM_AXES; firstInx++ ) {
                float firstAxisLen = textureMediator.getVolumeMicrometers()[ firstInx ].floatValue();
                int secondInx = (firstInx + 1) % NUM_AXES;
                float secondAxisLen = textureMediator.getVolumeMicrometers()[ secondInx ].floatValue();
                int thirdInx = (firstInx + 2) % NUM_AXES;
                float thirdAxisLen = textureMediator.getVolumeMicrometers()[ thirdInx ].floatValue();
                // compute number of slices
                int sliceCount = computeEffectiveAxisLen(firstInx);
                float slice0 = firstAxisLen / 2.0f + textureMediator.getVoxelMicrometers()[ firstInx ].floatValue();
                float direction = -1.0f;
                if ( firstInx > 2 ) {
                    direction = 1.0f;
                }
                float sliceSep = direction * textureMediator.getVoxelMicrometers()[ firstInx ].floatValue();

        		// Below "x", "y", and "z" actually refer to a1, a2, and a3, respectively;
                float second0 = secondAxisLen / 2.0f + textureMediator.getVoxelMicrometers()[secondInx].floatValue();
                float second1 = -second0;

                float third0 = thirdAxisLen / 2.0f + textureMediator.getVoxelMicrometers()[thirdInx].floatValue();
                float third1 = -third0;

                // Four points for four slice corners
                float[] p00 = {0,0,0};
                float[] p10 = {0,0,0};
                float[] p11 = {0,0,0};
                float[] p01 = {0,0,0};

                // reswizzle coordinate axes back to actual X, Y, Z (except x, saved for later)
                p00[ secondInx ] = p01[ secondInx ] = second0;
                p10[ secondInx ] = p11[ secondInx ] = second1;
                p00[ thirdInx ] = p10[ thirdInx ] = third0;
                p01[ thirdInx ] = p11[ thirdInx ] = third1;

                texCoordBuf[ firstInx ].rewind();
                short inxOffset = 0;
                for (int sliceInx = 0; sliceInx < sliceCount; ++sliceInx) {
                    // insert final coordinate into buffers

                    // FORWARD axes.
                    float sliceLoc = slice0 + (sliceInx * sliceSep);

                    // NOTE: only one of the three axes need change for each slice.  Other two remain same.
                    p00[ firstInx ] = p01[firstInx] = p10[firstInx] = p11[firstInx] = sliceLoc;
//                    if ( ( firstInx == 0 || firstInx == 3 ) && sliceInx > 200 ) {
//                        p00[ firstInx ] -= 200;
//                    }

                    addGeometry(firstInx, p00, p10, p11, p01);
                    addTextureCoords(
                            firstInx,
                            textureMediator.textureCoordFromVoxelCoord( p00 ),
                            textureMediator.textureCoordFromVoxelCoord( p01 ),
                            textureMediator.textureCoordFromVoxelCoord( p10 ),
                            textureMediator.textureCoordFromVoxelCoord( p11 )
                    );
                    if ( drawWithElements ) {
                        addIndices(firstInx, inxOffset);
                    }

                    // Now, take care of the negative-direction alternate to this buffer pair.
                    p00[ firstInx ] = p01[firstInx] = p10[firstInx] = p11[firstInx] = -sliceLoc;

                    addGeometry(firstInx + NUM_AXES, p00, p10, p11, p01);
                    addTextureCoords(
                            firstInx + NUM_AXES,
                            textureMediator.textureCoordFromVoxelCoord( p00 ),
                            textureMediator.textureCoordFromVoxelCoord( p01 ),
                            textureMediator.textureCoordFromVoxelCoord( p10 ),
                            textureMediator.textureCoordFromVoxelCoord( p11 )
                    );
                    if ( drawWithElements ) {
                        addIndices(firstInx + NUM_AXES, inxOffset);
                    }

                    inxOffset += 6;

                }

            }

        }
    }

    /**
     * If statical (no repeated-upload) is used for the vertices, then this may be called to reduce
     * memory.
     */
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
    public void enableBuffers(GL2 gl) throws Exception {
        geometryVertexBufferHandles = enableBuffersOfType(gl, geometryCoordBuf, GL2.GL_ARRAY_BUFFER);
        textureCoordBufferHandles = enableBuffersOfType(gl, texCoordBuf, GL2.GL_ARRAY_BUFFER);
        if ( drawWithElements ) {
            indexBufferHandles = enableBuffersOfType(gl, indexBuf, GL2.GL_ELEMENT_ARRAY_BUFFER);
        }
    }

    public void releaseBuffers(GL2 gl) {
        releaseBuffers(gl, geometryVertexBufferHandles);
        geometryVertexBufferHandles = null;
        releaseBuffers(gl, textureCoordBufferHandles);
        textureCoordBufferHandles = null;
        releaseBuffers(gl, indexBufferHandles);
        indexBufferHandles = null;
    }

    /**
     * Draw the contents of the buffer as needed. Call this from another draw method.
     *
     * @param gl an openGL object as provided during draw, init, etc.
     * @param axis an X,Y, or Z
     * @param direction inwards/outwards [-1.0, 1.0]
     */
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

    private int computeEffectiveAxisLen(int index) {
        double firstAxisLen = textureMediator.getVolumeMicrometers()[ index % 3 ];
        return (int)(0.5 + firstAxisLen / textureMediator.getVoxelMicrometers()[ index % 3 ]);
    }

    private void addIndices(int index, short inxOffset) {
        // Indices:  making six vertices from four definitions.
        indexBuf[ index ].put(inxOffset);
        indexBuf[ index ].put((short) (inxOffset + 1));
        indexBuf[ index ].put((short) (inxOffset + 2));

        indexBuf[ index ].put((short) (inxOffset + 1));
        indexBuf[ index ].put((short) (inxOffset + 3));
        indexBuf[ index ].put((short) (inxOffset + 2));
    }

    private void addTextureCoords(int index, float[] t00, float[] t01, float[] t10, float[] t11) {
        // Triangle 1
        texCoordBuf[ index ].put(t00);
        texCoordBuf[ index ].put(t10);
        texCoordBuf[ index ].put(t01);
        // Triangle 2
        texCoordBuf[ index ].put(t11);
    }

    private void addGeometry(int index, float[] p00p, float[] p10p, float[] p11p, float[] p01p) {
        // Only need four definitions.
        // Triangle 1
        geometryCoordBuf[ index ].put(p00p);
        geometryCoordBuf[ index ].put(p10p);
        geometryCoordBuf[ index ].put(p01p);
        // Triangle 2
        geometryCoordBuf[ index ].put(p11p);
    }

}
