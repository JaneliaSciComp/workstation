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
                float firstSegmentStart = segmentRanges.getRangeByAxisNum( firstInx )[ AxialSegmentRangeBean.LOW_INX ];
                float firstSegmentEnd = segmentRanges.getRangeByAxisNum( firstInx )[ AxialSegmentRangeBean.HIGH_INX ];
                float slice0 = (firstAxisLength - (firstAxisLength / 2.0f));
                float sliceSep = textureMediator.getVoxelMicrometers()[ firstInx ].floatValue();

        		// Below "x", "y", and "z" actually refer to a1, a2, and a3, respectively;
                float secondSegmentStart = segmentRanges.getRangeByAxisNum( secondInx )[ AxialSegmentRangeBean.LOW_INX ];
                float secondSegmentEnd = segmentRanges.getRangeByAxisNum( secondInx )[ AxialSegmentRangeBean.HIGH_INX ];
                float secondStart = secondSegmentStart - (secondAxisLength / 2.0f);
                float secondEnd = secondSegmentEnd - (secondAxisLength / 2.0f);

                float thirdSegmentStart = segmentRanges.getRangeByAxisNum( thirdInx )[ AxialSegmentRangeBean.LOW_INX ];
                float thirdSegmentEnd = segmentRanges.getRangeByAxisNum( thirdInx )[ AxialSegmentRangeBean.HIGH_INX ];
                float thirdStart = thirdSegmentStart - (thirdAxisLength / 2.0f);
                float thirdEnd = thirdSegmentEnd - (thirdAxisLength / 2.0f);

                // Four vertices for four slice corners
                float[] v00 = {0,0,0};   // conceptual 'lower left' 
                float[] v10 = {0,0,0};   // conceptual 'lower right'
                float[] v11 = {0,0,0};   // conceptual 'upper right'
                float[] v01 = {0,0,0};   // conceptual 'upper left'

                // reswizzle coordinate axes back to actual X, Y, Z (except x, saved for later)
                v00[ secondInx ] = v01[ secondInx ] = secondStart;
                v10[ secondInx ] = v11[ secondInx ] = secondEnd;
                v00[ thirdInx ] = v10[ thirdInx ] = thirdStart;
                v01[ thirdInx ] = v11[ thirdInx ] = thirdEnd;

                if ( logger.isDebugEnabled() ) {
                    logger.debug("Swizzled vertices, for axis {} are \n\t{}\n\t{}\n\t{}\n\t{}\n",
                        firstInx,
                        v00[0] + "," + v00[1] + "," + v00[2],
                        v10[0] + "," + v10[1] + "," + v10[2],
                        v11[0] + "," + v11[1] + "," + v11[2],
                        v01[0] + "," + v01[1] + "," + v01[2]
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
                    v00[ firstInx ] = v01[firstInx] = v10[firstInx] = v11[firstInx] = sliceLoc;

                    addGeometry(firstInx, v00, v10, v11, v01);
                    float[] t00 = textureMediator.textureCoordFromVoxelCoord( v00 );
                    float[] t01 = textureMediator.textureCoordFromVoxelCoord( v01 );
                    float[] t10 = textureMediator.textureCoordFromVoxelCoord( v10 );
                    float[] t11 = textureMediator.textureCoordFromVoxelCoord( v11 );
                    if ( firstInx == 2 )
                        checkGeometry( v00, v01, v10, v11, t00, t01, t10, t11 );
                    
                    addTextureCoords( firstInx, t00, t01, t10, t11 );
                    addIndices(firstInx, inxOffset);

                    // Now, take care of the negative-direction alternate to this buffer pair.
                    v00[ firstInx ] = v01[firstInx] = v10[firstInx] = v11[firstInx] = sliceLoc; //-sliceLoc.  Later: subtract this from the max slice.

                    addGeometry(firstInx + NUM_AXES, v00, v10, v11, v01);
                    t00 = textureMediator.textureCoordFromVoxelCoord( v00 );
                    t01 = textureMediator.textureCoordFromVoxelCoord( v01 );
                    t10 = textureMediator.textureCoordFromVoxelCoord( v10 );
                    t11 = textureMediator.textureCoordFromVoxelCoord( v11 );
//                    if ( firstInx == 2 )
//                        checkGeometry( v00, v01, v10, v11, t00, t01, t10, t11 );
                    
                    addTextureCoords( firstInx + NUM_AXES, t00, t01, t10, t11 );
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
