package org.janelia.it.workstation.gui.alignment_board_viewer.buffering;

import org.janelia.it.workstation.gui.viewer3d.texture.TextureMediator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.*;
import org.janelia.it.workstation.gui.viewer3d.buffering.AbstractCoordBufMgr;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 6/14/13
 * Time: 10:17 AM
 *
 * This delegate/helper handles buffering required for 3D rendering.
 */
public class SegmentedVtxCoordBufMgr extends AbstractCoordBufMgr {
    private static final int NUM_BUFFERS_PER_TYPE = 3 * 2; // 3=x,y,z;  2=pos,neg

    private AxialSegmentRangeBean segmentRanges;
    private final Logger logger = LoggerFactory.getLogger(SegmentedVtxCoordBufMgr.class);

    public SegmentedVtxCoordBufMgr() {
    }

    public SegmentedVtxCoordBufMgr( TextureMediator textureMediator ) {
        this();
        setTextureMediator( textureMediator );
    }
    
    /**
     * @param segmentRanges the segmentRanges to set
     */
    public void setSegmentRanges(AxialSegmentRangeBean segmentRanges) {
        this.segmentRanges = segmentRanges;
    }

    @Override
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
    @Override
    public void buildBuffers() {
        vertexCountByAxis = new int[ NUM_AXES ];
        if ( texCoordBuf[ 0 ] == null  &&  textureMediator != null ) {
            allocateBuffers();

            // Now produce the vertexes to stuff into all of the buffers.
            //  Making sets of four.
            for ( int firstInx = 0; firstInx < NUM_AXES; firstInx++ ) {
                int secondInx = (firstInx + 1) % NUM_AXES;
                int thirdInx = (firstInx + 2) % NUM_AXES;

                float firstAxisLength = textureMediator.getVolumeMicrometers()[ firstInx ].floatValue();
                float secondAxisLength = textureMediator.getVolumeMicrometers()[ secondInx ].floatValue();
                float thirdAxisLength = textureMediator.getVolumeMicrometers()[ thirdInx ].floatValue();                
                
                // compute number of slices
                float firstSegmentStart = segmentRanges.getRangeByAxisNum( firstInx )[ AxialSegmentRangeBean.HIGH_INX ];
                float firstSegmentEnd = segmentRanges.getRangeByAxisNum( firstInx )[ AxialSegmentRangeBean.LOW_INX ];
                float slice0 = (firstAxisLength - (firstAxisLength / 2.0f));
                float sliceSep = textureMediator.getVoxelMicrometers()[ firstInx ].floatValue();

        		// Below "x", "y", and "z" actually refer to a1, a2, and a3, respectively;
                float secondSegmentStart = segmentRanges.getRangeByAxisNum( secondInx )[ AxialSegmentRangeBean.HIGH_INX ];
                float secondSegmentEnd = segmentRanges.getRangeByAxisNum( secondInx )[ AxialSegmentRangeBean.LOW_INX ];
                float secondStart = secondSegmentStart - (secondAxisLength / 2.0f);
                float secondEnd = secondSegmentEnd - (secondAxisLength / 2.0f);

                float thirdSegmentStart = segmentRanges.getRangeByAxisNum( thirdInx )[ AxialSegmentRangeBean.HIGH_INX ];
                float thirdSegmentEnd = segmentRanges.getRangeByAxisNum( thirdInx )[ AxialSegmentRangeBean.LOW_INX ];
                float thirdStart = thirdSegmentStart - (thirdAxisLength / 2.0f);
                float thirdEnd = thirdSegmentEnd - (thirdAxisLength / 2.0f);

                // Four points for four slice corners
                float[] p00 = {0,0,0};
                float[] p10 = {0,0,0};
                float[] p11 = {0,0,0};
                float[] p01 = {0,0,0};

                // reswizzle coordinate axes back to actual X, Y, Z (except x, saved for later)
                p00[ secondInx ] = p01[ secondInx ] = secondStart;
                p10[ secondInx ] = p11[ secondInx ] = secondEnd;
                p00[ thirdInx ] = p10[ thirdInx ] = thirdStart;
                p01[ thirdInx ] = p11[ thirdInx ] = thirdEnd;

                if ( logger.isDebugEnabled() ) {
                    logger.debug("Swizzled points, for axis {} are \n\t{}\n\t{}\n\t{}\n\t{}\n",
                        firstInx,
                        p00[0] + "," + p00[1] + "," + p00[2],
                        p10[0] + "," + p10[1] + "," + p10[2],
                        p11[0] + "," + p11[1] + "," + p11[2],
                        p01[0] + "," + p01[1] + "," + p01[2]
                    );
                }
                
                texCoordBuf[ firstInx ].rewind();
                short inxOffset = 0;
                int[] range = segmentRanges.getRangeByAxisNum(firstInx);
//                for (int sliceInx = range[AxialSegmentRangeBean.HIGH_INX]; sliceInx > range[AxialSegmentRangeBean.LOW_INX]; --sliceInx) {
                for (int sliceInx = range[AxialSegmentRangeBean.LOW_INX]; sliceInx < range[AxialSegmentRangeBean.HIGH_INX]; ++sliceInx) {
                    // insert final coordinate into buffers

                    // FORWARD axes.
                    float sliceLoc = (sliceInx * sliceSep);
                    if (firstInx == 2)
                        logger.info("For {}, have sliceInx={}, slice0={}, sliceSep={}, sliceLoc={}.", firstInx, sliceInx, slice0, sliceSep, sliceLoc);

                    // NOTE: only one of the three axes need change for each slice.  Other two remain same.
                    p00[ firstInx ] = p01[firstInx] = p10[firstInx] = p11[firstInx] = sliceLoc;

                    addGeometry(firstInx, p00, p10, p11, p01);
                    addTextureCoords(
                            firstInx,
                            textureMediator.textureCoordFromVoxelCoord( p00 ),
                            textureMediator.textureCoordFromVoxelCoord( p01 ),
                            textureMediator.textureCoordFromVoxelCoord( p10 ),
                            textureMediator.textureCoordFromVoxelCoord( p11 )
                    );
                    addIndices(firstInx, inxOffset);

                    // Now, take care of the negative-direction alternate to this buffer pair.
                    p00[ firstInx ] = p01[firstInx] = p10[firstInx] = p11[firstInx] = sliceLoc; //-sliceLoc.  Later: subtract this from the max slice.

                    addGeometry(firstInx + NUM_AXES, p00, p10, p11, p01);
                    addTextureCoords(
                            firstInx + NUM_AXES,
                            textureMediator.textureCoordFromVoxelCoord( p00 ),
                            textureMediator.textureCoordFromVoxelCoord( p01 ),
                            textureMediator.textureCoordFromVoxelCoord( p10 ),
                            textureMediator.textureCoordFromVoxelCoord( p11 )
                    );
                    addIndices(firstInx + NUM_AXES, inxOffset);

                    inxOffset += 6;

                }

            }

        }
    }

    @Override
    protected int computeEffectiveAxisLen(int index) {
        final int[] range = segmentRanges.getRangeByAxisNum(index);
        return range[ AxialSegmentRangeBean.HIGH_INX ] - range[ AxialSegmentRangeBean.LOW_INX ];
    }

    private void allocateBuffers() {
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
            ByteBuffer inxByteBuffer = ByteBuffer.allocateDirect(numVertices * Short.SIZE / 8);
            inxByteBuffer.order(ByteOrder.nativeOrder());
            indexBuf[ i ] = inxByteBuffer.asShortBuffer();
        }
    }

}
