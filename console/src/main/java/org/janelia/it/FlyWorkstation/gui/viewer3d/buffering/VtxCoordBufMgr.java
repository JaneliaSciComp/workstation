package org.janelia.it.FlyWorkstation.gui.viewer3d.buffering;

import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureMediator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL2;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

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
    public static final int VERTICES_PER_SLICE = 6;
    public static final int COORDS_PER_VERTEX = 3;
    public static final int NUM_AXES = 3;

    // Buffer objects for setting geometry on the GPU side.
    //   I need one per starting direction (x,y,z) times one for positive, one for negative.
    //
    private FloatBuffer texCoordBuf[] = new FloatBuffer[ NUM_BUFFERS_PER_TYPE ];
    private FloatBuffer geometryCoordBuf[] = new FloatBuffer[ NUM_BUFFERS_PER_TYPE ];

    private int[] vertexCountByAxis;

    // Int pointers to use as handles when dealing with GPU.
    private int[] geometryVertexBufferHandles;
    private int[] textureCoordBufferHandles;

    private TextureMediator textureMediator;

    private Logger logger = LoggerFactory.getLogger(VtxCoordBufMgr.class);

    public VtxCoordBufMgr() {
    }

    public VtxCoordBufMgr( TextureMediator textureMediator ) {
        this();
        setTextureMediator( textureMediator );
    }

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
    public void buildBuffers() {
        /*
        // compute number of slices
		int sx = (int)(0.5 + signalTextureMediator.getVolumeMicrometers()[a1.index()] / signalTextureMediator.getVoxelMicrometers()[a1.index()]);
		// compute position of first slice
		double x0 = -direction * (signalTextureMediator.getVoxelMicrometers()[a1.index()] - signalTextureMediator.getVolumeMicrometers()[a1.index()]) / 2.0;
		// compute distance between slices
		double dx = -direction * signalTextureMediator.getVoxelMicrometers()[a1.index()];

         */
        vertexCountByAxis = new int[ NUM_AXES ];
        if ( texCoordBuf[ 0 ] == null  &&  textureMediator != null ) {
            // Compute sizes, and allocate buffers.
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
                float sliceSep = textureMediator.getVoxelMicrometers()[ firstInx ].floatValue();

        		// Below "x", "y", and "z" actually refer to a1, a2, and a3, respectively;
                float second0 = secondAxisLen / 2.0f + textureMediator.getVoxelMicrometers()[secondInx].floatValue();
                float second1 = -second0;

                float third0 = thirdAxisLen / 2.0f + textureMediator.getVoxelMicrometers()[thirdInx].floatValue();
                float third1 = -third0;

                // Four points for four slice corners
                float[] p00p = {0,0,0};
                float[] p10p = {0,0,0};
                float[] p11p = {0,0,0};
                float[] p01p = {0,0,0};

                // reswizzle coordinate axes back to actual X, Y, Z (except x, saved for later)
                p00p[ secondInx ] = p01p[ secondInx ] = second0;
                p10p[ secondInx ] = p11p[ secondInx ] = second1;
                p00p[ thirdInx ] = p10p[ thirdInx ] = third0;
                p01p[ thirdInx ] = p11p[ thirdInx ] = third1;

                // Four negated points for four slice corners
                float[] p00n = {0,0,0};
                float[] p10n = {0,0,0};
                float[] p11n = {0,0,0};
                float[] p01n = {0,0,0};

                // reswizzle coordinate axes back to actual X, Y, Z (except x, saved for later)
                p00n[ secondInx ] = p01n[ secondInx ] = -second0;
                p10n[ secondInx ] = p11n[ secondInx ] = -second1;
                p00n[ thirdInx ] = p10n[ thirdInx ] = -third0;
                p01n[ thirdInx ] = p11n[ thirdInx ] = -third1;

                texCoordBuf[ firstInx ].rewind();
                for (int sliceInx = 0; sliceInx < sliceCount; ++sliceInx) {
                    // insert final coordinate into buffers

                    // FORWARD axes.
                    float sliceLoc = slice0 + sliceInx * sliceSep;
                    p00p[ firstInx ] = p01p[firstInx] = p10p[firstInx] = p11p[firstInx] = sliceLoc;

                    float[] t00 = textureMediator.textureCoordFromVoxelCoord( p00p );
                    float[] t01 = textureMediator.textureCoordFromVoxelCoord( p01p );
                    float[] t10 = textureMediator.textureCoordFromVoxelCoord( p10p );
                    float[] t11 = textureMediator.textureCoordFromVoxelCoord( p11p );

                    // Only need four definitions, but making six vertexes.
                    // Triangle 1
                    texCoordBuf[ firstInx ].put( p00p );
                    texCoordBuf[ firstInx ].put( p10p );
                    texCoordBuf[ firstInx ].put( p01p );
                    // Triangle 2
                    texCoordBuf[ firstInx ].put( p10p );
                    texCoordBuf[ firstInx ].put( p11p );
                    texCoordBuf[ firstInx ].put( p01p );

                    // Triangle 1
                    geometryCoordBuf[ firstInx ].put( t00 );
                    geometryCoordBuf[ firstInx ].put( t10 );
                    geometryCoordBuf[ firstInx ].put( t01 );
                    // Triangle 2
                    geometryCoordBuf[ firstInx ].put( t10 );
                    geometryCoordBuf[ firstInx ].put( t11 );
                    geometryCoordBuf[ firstInx ].put( t01 );

                    // Now, take care of the negative-direction alternate to this buffer pair.
                    t00 = textureMediator.textureCoordFromVoxelCoord( p00n );
                    t01 = textureMediator.textureCoordFromVoxelCoord( p01n );
                    t10 = textureMediator.textureCoordFromVoxelCoord( p10n );
                    t11 = textureMediator.textureCoordFromVoxelCoord( p11n );

                    // Triangle 1
                    texCoordBuf[ firstInx + NUM_AXES ].put( p00n );
                    texCoordBuf[ firstInx + NUM_AXES ].put( p10n );
                    texCoordBuf[ firstInx + NUM_AXES ].put( p01n );
                    // Triangle 2
                    texCoordBuf[ firstInx + NUM_AXES ].put( p10p );
                    texCoordBuf[ firstInx + NUM_AXES ].put( p11p );
                    texCoordBuf[ firstInx + NUM_AXES ].put( p01p );

                    // Triangle 1
                    geometryCoordBuf[ firstInx + NUM_AXES ].put( t00 );
                    geometryCoordBuf[ firstInx + NUM_AXES ].put( t10 );
                    geometryCoordBuf[ firstInx + NUM_AXES ].put( t01 );
                    // Triangle 2
                    geometryCoordBuf[ firstInx + NUM_AXES ].put( t10 );
                    geometryCoordBuf[ firstInx + NUM_AXES ].put( t11 );
                    geometryCoordBuf[ firstInx + NUM_AXES ].put( t01 );

                }

            }

        }
    }

    /**
     * To use uploaded vertex and coordinate data, it must first be uploaded, and enabled.  Its role must
     * be designated, and pointers need to be saved.
     *
     * @param gl for graphic-oriented operations.
     * @throws Exception thrown by any called code.
     */
    public void enableBuffers(GL2 gl) throws Exception {
        geometryVertexBufferHandles = enableBuffersOfType(gl, geometryCoordBuf);
        textureCoordBufferHandles = enableBuffersOfType(gl, texCoordBuf);
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
        gl.glFrontFace(GL2.GL_CCW);

        // Point to the right vertex set.
        int handle = bindBuffer( gl, axis, geometryVertexBufferHandles, direction );

        // 3 floats per vertex.  Stride is 0, offset to first is 0.
        gl.glEnableClientState( GL2.GL_VERTEX_ARRAY );
        gl.glVertexPointer( 3, GL2.GL_FLOAT, 0, 0 );

        // Point to the right texture coordinate set.
        handle = bindBuffer( gl, axis, textureCoordBufferHandles, direction );
        gl.glEnableClientState( GL2.GL_TEXTURE_COORD_ARRAY );
        // 3 floats per texture coord.  Stride is 0, offset to first is 0.
        gl.glTexCoordPointer( 3, GL2.GL_FLOAT, 0, 0 );

        gl.glDrawArrays(GL2.GL_TRIANGLES, 0, getVertexCount( axis ));

        gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
    }

    /** Convenience method to cut down on repeated code. */
    private int[] enableBuffersOfType(GL2 gl, FloatBuffer[] buffers ) throws Exception {
        // Make handles for subsequent use.
        int[] rtnVal = new int[ NUM_BUFFERS_PER_TYPE ];
        gl.glGenBuffers( 6, rtnVal, 0 );

        // DEBUG
        System.out.println("DUMPING THE BUFFERS");

        // Bind data to the handles, and upload it to the GPU.
        for ( int i = 0; i < NUM_BUFFERS_PER_TYPE; i++ ) {
            System.out.println("BUFFER " + i);
            buffers[ i ].rewind();
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, rtnVal[ i ]);
            // NOTE: this operates on the most recent "bind".  Therefore unless
            // synchronization is in use, this makes the method un-thread-safe.
            // Imagine multiple "bind" and "buffer-data" calls in parallel threads...disaster!
            gl.glBufferData(
                    GL2.GL_ARRAY_BUFFER,
                    (long)(buffers[ i ].capacity()),
                    buffers[ i ],
                    GL2.GL_STATIC_DRAW
            );

            // DEBUG
//            buffers[ i ].rewind();
//            for (int j = 0; j < buffers[i].capacity(); j++) {
//                float f = buffers[i].get();
//                System.out.print( f + " " );
//            }
//            System.out.println();
        }

        return rtnVal;
    }

    /**
     * Here the buffer is actually used, to establish the vertices and texture coordinates for the display.
     *
     * @param axis tells the primary axis.
     * @param direction for positive/negative view perspective.
     */
    private int bindBuffer( GL2 gl, CoordinateAxis axis, int[] handles, double direction ) {
        if ( axis == null  ||  (direction != 1.0  &&  direction != -1.0) ) {
            logger.error("Failed to bind buffer for {}, {}.", axis.getName(), direction);
            return -1;
        }
        else {
            // Change direction from -1.0 or 1.0 to 0 or 3 as offset into arrays.
            int directionOffset = 0;
            if ( direction == -1.0 ) {
                directionOffset = 3;
            }

            int bufferOffset = directionOffset + axis.index();
            gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, handles[ bufferOffset ] );

            logger.debug("Returning buffer offset of {} for {}.", bufferOffset, axis.getName() + ":" + direction);
            return handles[ bufferOffset ];
        }

    }

    private int getVertexCount( CoordinateAxis axis ) {
        return vertexCountByAxis[ axis.index() ];
    }

    private int computeEffectiveAxisLen(int firstInx) {
        double firstAxisLen = textureMediator.getVolumeMicrometers()[ firstInx % 3 ];
        return (int)(0.5 + firstAxisLen / textureMediator.getVoxelMicrometers()[ firstInx % 3 ]);
    }

}
