package org.janelia.it.FlyWorkstation.gui.viewer3d.buffering;

import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureMediator;

import javax.media.opengl.GL2;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;

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

    // Buffer objects for setting geometry on the GPU side.  Trying GL_TRIANGLE_STRIP at first.
    //   I need one per starting direction (x,y,z) times one for positive, one for negative.
    //
    private DoubleBuffer texCoordBuf[] = new DoubleBuffer[ NUM_BUFFERS_PER_TYPE ];
    private DoubleBuffer geometryCoordBuf[] = new DoubleBuffer[ NUM_BUFFERS_PER_TYPE ];

    private int[] vertexCountByAxis;

    // Int pointers to use as handles when dealing with GPU.
    private int[] geometryVertexBufferHandles;
    private int[] textureCoordBufferHandles;

    private TextureMediator textureMediator;

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
     *
     * NOTE: where I am, I need to work when to upload the buffers, and how to differentiate texture vertex and
     *       geometric vertex buffer values.  The calls are likely  glVertexAttribPointer and glEnableVertexAttribArray,
     *       followed by glDrawArrays with argument of GL_TRIANGLE_STRIP.  Example seems to upload prior.
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
        vertexCountByAxis = new int[ 3 ];
        if ( texCoordBuf[ 0 ] == null  &&  textureMediator != null ) {
            // Compute sizes, and allocate buffers.
            for ( int i = 0; i < NUM_BUFFERS_PER_TYPE; i++ ) {
                // Three coords per vertex.  Four vertexes per slice.  Times number of slices.
                int numVertices = 4 * computeEffectiveAxisLen(i % 3);
                int numCoords = 3 * numVertices;
                vertexCountByAxis[ i % 3 ] = numVertices;

                // NOTE on usage.  Here, 'allocateDirect' is used, because otherwise there would be no backing array.
                //  However, this is not guaranteed by the Java NIO contract.  Hence under some circumstances,
                //  this may not be true.  Wrapping a backing array did not work on my system.
                //  Further, the use of a byte-buffer is required because of the different endian-ness between
                //  Java and the native system.
                ByteBuffer texByteBuffer = ByteBuffer.allocateDirect(numCoords * Double.SIZE / 8);
                texByteBuffer.order( ByteOrder.nativeOrder() );
                texCoordBuf[ i ] = texByteBuffer.asDoubleBuffer();

                ByteBuffer geoByteBuffer = ByteBuffer.allocateDirect(numCoords * Double.SIZE / 8);
                geoByteBuffer.order(ByteOrder.nativeOrder());
                geometryCoordBuf[ i ] = geoByteBuffer.asDoubleBuffer();
            }

            // Now produce the vertexes to stuff into all of the buffers.
            //  Making sets of four.
            // FORWARD axes.
            for ( int firstInx = 0; firstInx < 3; firstInx++ ) {
                double firstAxisLen = textureMediator.getVolumeMicrometers()[ firstInx ];
                int secondInx = (firstInx + 1) % 3;
                double secondAxisLen = textureMediator.getVolumeMicrometers()[ secondInx ];
                int thirdInx = (firstInx + 2) % 3;
                double thirdAxisLen = textureMediator.getVolumeMicrometers()[ thirdInx ];
                // compute number of slices
                int sliceCount = computeEffectiveAxisLen(firstInx);
                double slice0 = (textureMediator.getVoxelMicrometers()[ firstInx ] - firstAxisLen) / 2.0;
                double sliceSep = textureMediator.getVoxelMicrometers()[ firstInx ];

                /*
                		// Below "x", "y", and "z" actually refer to a1, a2, and a3, respectively;
		// These axes might be permuted from the real world XYZ
		// Each slice cuts through an exact voxel center,
		// but slice edges extend all the way to voxel edges.
		// Compute dimensions of one x slice: y0-y1 and z0-z1
		// Pad edges of rectangles by one voxel to support oblique ray tracing past edges
		double y0 = direction * (signalTextureMediator.getVolumeMicrometers()[a2.index()] / 2.0 + signalTextureMediator.getVoxelMicrometers()[a2.index()]);
		double y1 = -y0;
		double z0 = direction * (signalTextureMediator.getVolumeMicrometers()[a3.index()] / 2.0 + signalTextureMediator.getVoxelMicrometers()[a3.index()]);
		double z1 = -z0;

                 */
                double second0 = secondAxisLen / 2.0 + textureMediator.getVoxelMicrometers()[secondInx];
                double second1 = -second0;

                double third0 = thirdAxisLen / 2.0 + textureMediator.getVoxelMicrometers()[thirdInx];
                double third1 = -third0;

                // Four points for four slice corners
                double[] p00p = {0,0,0};
                double[] p10p = {0,0,0};
                double[] p11p = {0,0,0};
                double[] p01p = {0,0,0};

                // reswizzle coordinate axes back to actual X, Y, Z (except x, saved for later)
                p00p[ secondInx ] = p01p[ secondInx ] = second0;
                p10p[ secondInx ] = p11p[ secondInx ] = second1;
                p00p[ thirdInx ] = p10p[ thirdInx ] = third0;
                p01p[ thirdInx ] = p11p[ thirdInx ] = third1;

                // Four negated points for four slice corners
                double[] p00n = {0,0,0};
                double[] p10n = {0,0,0};
                double[] p11n = {0,0,0};
                double[] p01n = {0,0,0};

                // reswizzle coordinate axes back to actual X, Y, Z (except x, saved for later)
                p00n[ secondInx ] = p01n[ secondInx ] = -second0;
                p10n[ secondInx ] = p11n[ secondInx ] = -second1;
                p00n[ thirdInx ] = p10n[ thirdInx ] = -third0;
                p01n[ thirdInx ] = p11n[ thirdInx ] = -third1;

                texCoordBuf[ firstInx ].rewind();
                for (int sliceInx = 0; sliceInx < sliceCount; ++sliceInx) {
                    // insert final coordinate into buffers
                    double sliceLoc = slice0 + sliceInx * sliceSep;
                    p00p[ firstInx ] = p01p[firstInx] = p10p[firstInx] = p11p[firstInx] = sliceLoc;

                    double[] t00 = textureMediator.textureCoordFromVoxelCoord( p00p );
                    double[] t01 = textureMediator.textureCoordFromVoxelCoord( p01p );
                    double[] t10 = textureMediator.textureCoordFromVoxelCoord( p10p );
                    double[] t11 = textureMediator.textureCoordFromVoxelCoord( p11p );


                    /*
                    			// Compute texture coordinates
			// color from black(back) to white(front) for debugging.
			// double c = xi / (double)sx;
			// gl.glColor3d(c, c, c);
			// draw the quadrilateral as a triangle strip with 4 points
            // (somehow GL_QUADS never works correctly for me)
            setTextureCoordinates(gl, t00[0], t00[1], t00[2]);
            gl.glVertex3d(p00[0], p00[1], p00[2]);
            setTextureCoordinates(gl, t10[0], t10[1], t10[2]);
            gl.glVertex3d(p10[0], p10[1], p10[2]);
            setTextureCoordinates(gl, t01[0], t01[1], t01[2]);
            gl.glVertex3d(p01[0], p01[1], p01[2]);
            setTextureCoordinates(gl, t11[0], t11[1], t11[2]);
            gl.glVertex3d(p11[0], p11[1], p11[2]);

                     */
                    texCoordBuf[ firstInx ].put( p00p );
                    texCoordBuf[ firstInx ].put( p10p );
                    texCoordBuf[ firstInx ].put( p01p );
                    texCoordBuf[ firstInx ].put( p11p );

                    geometryCoordBuf[ firstInx ].put( t00 );
                    geometryCoordBuf[ firstInx ].put( t10 );
                    geometryCoordBuf[ firstInx ].put( t01 );
                    geometryCoordBuf[ firstInx ].put( t11 );

                    // Now, take care of the negative-direction alternate to this buffer pair.
                    t00 = textureMediator.textureCoordFromVoxelCoord( p00n );
                    t01 = textureMediator.textureCoordFromVoxelCoord( p01n );
                    t10 = textureMediator.textureCoordFromVoxelCoord( p10n );
                    t11 = textureMediator.textureCoordFromVoxelCoord( p11n );

                    texCoordBuf[ firstInx + 3 ].put( p00n );
                    texCoordBuf[ firstInx + 3 ].put( p10n );
                    texCoordBuf[ firstInx + 3 ].put( p01n );
                    texCoordBuf[ firstInx + 3 ].put( p11n );

                    geometryCoordBuf[ firstInx + 3 ].put( t00 );
                    geometryCoordBuf[ firstInx + 3 ].put( t10 );
                    geometryCoordBuf[ firstInx + 3 ].put( t01 );
                    geometryCoordBuf[ firstInx + 3 ].put( t11 );

                }

                /*
                		for (int xi = 0; xi < sx; ++xi) {
			// insert final coordinate into corner vectors
			double x = x0 + xi * dx;
			int a = a1.index();
			p00[a] = p01[a] = p10[a] = p11[a] = x;

                 */

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
        // Point to the right vertex set.
        int handle = bindBuffer( gl, axis, geometryVertexBufferHandles, direction );
        gl.glEnableClientState( GL2.GL_VERTEX_ARRAY );
        // 3 doubles per vertex.  Stride is 0, offset to first is 0.
        //   NOTE, omitting this creates a SIGSEGV segmentation fault on Mac
        gl.glVertexPointer( 3, GL2.GL_DOUBLE, 0, 0 );

        // Point to the right texture coordinate set.
        handle = bindBuffer( gl, axis, textureCoordBufferHandles, direction );
        gl.glEnableClientState( GL2.GL_TEXTURE_COORD_ARRAY );
        // 3 doubles per texture coord.  Stride is 0, offset to first is 0.
        gl.glTexCoordPointer( 3, GL2.GL_DOUBLE, 0, 0 );

        gl.glDrawArrays(GL2.GL_TRIANGLE_STRIP, 0, getVertexCount( axis ));

        gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
    }

    /** Convenience method to cut down on repeated code. */
    private int[] enableBuffersOfType(GL2 gl, DoubleBuffer[] buffers ) throws Exception {
        // Make handles for subsequent use.
        int[] rtnVal = new int[ NUM_BUFFERS_PER_TYPE ];
        gl.glGenBuffers( 6, rtnVal, 0 );

        // Bind data to the handles, and upload it to the GPU.
        for ( int i = 0; i < NUM_BUFFERS_PER_TYPE; i++ ) {
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

            return handles[ bufferOffset ];
        }

    }

    private int getVertexCount( CoordinateAxis axis ) {
        return vertexCountByAxis[ axis.index() ];
    }

    private int computeEffectiveAxisLen(int firstInx) {
        double firstAxisLen = textureMediator.getVolumeMicrometers()[ firstInx % 3 ];
        return (int)(0.5 + firstAxisLen / textureMediator.getVoxelMicrometers()[ firstInx ]);
    }

}
