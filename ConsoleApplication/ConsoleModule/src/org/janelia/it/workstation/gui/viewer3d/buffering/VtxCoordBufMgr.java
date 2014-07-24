package org.janelia.it.workstation.gui.viewer3d.buffering;

import org.janelia.it.workstation.gui.viewer3d.texture.TextureMediator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.*;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 6/14/13
 * Time: 10:17 AM
 *
 * This delegate/helper handles buffering required for 3D rendering.
 */
public class VtxCoordBufMgr extends AbstractCoordBufMgr {
    private static final int NUM_BUFFERS_PER_TYPE = 3 * 2; // 3=x,y,z;  2=pos,neg

    private final Logger logger = LoggerFactory.getLogger(VtxCoordBufMgr.class);

    public VtxCoordBufMgr() {
    }

    public VtxCoordBufMgr( TextureMediator textureMediator ) {
        this();
        setTextureMediator( textureMediator );
    }        

    /**
     * This method builds the buffers of vertices for both geometry and texture.  These are calculated similarly,
     * but with different ranges.  There are multiple such buffers of both types, and they are kept in arrays.
     * The arrays are indexed as follows:
     * 1. Offsets [ 0..2 ] are for positive direction.
     * 2. Offsets 0,3 are X; 1,4 are Y and 2,5 are Z.
     */
    @Override
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

                    addGeometry(firstInx, p00, p10, p11, p01);
                    float[] t00 = textureMediator.textureCoordFromVoxelCoord(p00);
                    float[] t01 = textureMediator.textureCoordFromVoxelCoord(p01);
                    float[] t10 = textureMediator.textureCoordFromVoxelCoord(p10);
                    float[] t11 = textureMediator.textureCoordFromVoxelCoord(p11);
                    addTextureCoords(
                            firstInx,
                            t00,
                            t01,
                            t10,
                            t11
                    );
                    if ( drawWithElements ) {
                        addIndices(firstInx, inxOffset);
                    }

                    // Now, take care of the negative-direction opposite to this buffer pair.
                    p00[ firstInx ] = p01[firstInx] = p10[firstInx] = p11[firstInx] = -sliceLoc;

                    addGeometry(firstInx + NUM_AXES, p00, p10, p11, p01);
                    t00 = textureMediator.textureCoordFromVoxelCoord(p00);
                    t01 = textureMediator.textureCoordFromVoxelCoord(p01);
                    t10 = textureMediator.textureCoordFromVoxelCoord(p10);
                    t11 = textureMediator.textureCoordFromVoxelCoord(p11);
                    addTextureCoords(
                            firstInx + NUM_AXES,
                            t00,
                            t01,
                            t10,
                            t11
                    );
                    if ( drawWithElements ) {
                        addIndices(firstInx + NUM_AXES, inxOffset);
                    }

                    if ( firstInx == 2 ) {
                        logger.info("For Negative {}, have sliceInx={}, slice0={}, sliceSep={}, sliceLoc={}.", firstInx, sliceInx, slice0, sliceSep, sliceLoc);
                        checkGeometry(p00, p01, p10, p11, t00, t01, t10, t11);
                    }

                    inxOffset += 6;

                }

            }

        }
    }

    /**
     * Debug code: given the definitions for positions and textures, dump em.
     * @param v00 vertex at 'lower left' corner
     * @param v01 vertex at 'upper left' corner
     * @param v10 vertex at 'lower right' corner
     * @param v11 vertex at 'upper right' corner
     * @param t00 tex coord at 'lower left' corner
     * @param t01 tex coord at 'upper left' corner
     * @param t10 tex coord at 'lower right' corner
     * @param t11 tex coord at 'upper right' corner
     */
    private void checkGeometry(
            float[] v00, float[] v01, float[] v10, float[] v11,
            float[] t00, float[] t01, float[] t10, float[] t11
    ) {
        logger.info( String.format( "Vertex %s: tex-coord %s 'lower left'", expandFloatArray(v00), expandFloatArray(t00 ) ) );
        logger.info( String.format( "Vertex %s: tex-coord %s 'upper left'", expandFloatArray(v01), expandFloatArray(t01 ) ) );
        logger.info( String.format( "Vertex %s: tex-coord %s 'lower right'", expandFloatArray(v10), expandFloatArray(t10 ) ) );
        logger.info( String.format( "Vertex %s: tex-coord %s 'upper right'", expandFloatArray(v11), expandFloatArray(t11 ) ) );
    }

    private String expandFloatArray( float[] members ) {
        StringBuilder bldr = new StringBuilder("[");
        for ( float member: members ) {
            if ( bldr.length() > 1 ) {
                bldr.append(",");
            }
            bldr.append(member);
        }
        bldr.append("]");
        return bldr.toString();
    }

}
