package org.janelia.it.FlyWorkstation.gui.viewer3d.loader;

import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/13/13
 * Time: 4:21 PM
 *
 * This implementation of a mask builder takes renderables as its driving data.  It will accept the renderables,
 * along with their applicable chunks of data, to produce its texture data volume, in memory.
 */
public class MaskChanSingleFileLoader {

    public static final int REQUIRED_AXIAL_LENGTH_DIVISIBLE = 4;
    private static final int FLOAT_BYTES = Float.SIZE / 8;
    private static final int LONG_BYTES = Long.SIZE / 8;
    private long sx;
    private long sy;
    private long sz;

    private Long[] volumeVoxels;
    private float[] coordCoverage;

    // These values are kept here for future reference.
    //  These bounds tell the extent of the overall space, that the current renderable occupies.
    private Long[] boundsXCoords;
    private Long[] boundsYCoords;
    private Long[] boundsZCoords;
    //  These "microns" tell the extent of real-world space occupied by a single 3D point (or voxel).
    private float xMicrons;
    private float yMicrons;
    private float zMicrons;

    private long fastestSrcVaryingMax;
    private long secondFastestSrcVaryingMax;
    private long secondFastestSrcVaryingCoord;
    private long slowestSrcVaryingMax;
    private long slowestSrcVaryingCoord;

    private Byte axis;

    private ChannelMetaData channelMetaData;

    private int dimensionOrder = -1;
    private Long totalVoxels;

    private Collection<MaskChanDataAcceptorI> maskAcceptors;
    private Collection<MaskChanDataAcceptorI> channelAcceptors;
    private RenderableBean renderableBean;

    // The input data is known to be little-endian or LSB.
    private byte[] longArray = new byte[ 8 ];
    private byte[] floatArray = new byte[ 4 ];

    private ByteBuffer longBuffer = ByteBuffer.wrap( longArray );
    {
        longBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    private ByteBuffer floatBuffer = ByteBuffer.wrap( floatArray );
    {
        floatBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    // Tells how many rays have been passed over up to the current add call.
    private long latestRayNumber = 0;   // This is evolving state: it depends on previously-updated data.
    private int cummulativeBytesReadCount = 0;  // evolving state.

    private Logger logger = LoggerFactory.getLogger( MaskChanSingleFileLoader.class );

    /**
     * Construct a file loader for all data about a single renderable, and with all targets for that data.
     *
     * @param maskAcceptors these care about mask data per se.
     * @param channelAcceptors this care about the channel data to which mask data refers.
     * @param renderableBean all actions taken here are concerning this renderable.
     */
    public MaskChanSingleFileLoader(
            Collection<MaskChanDataAcceptorI> maskAcceptors,
            Collection<MaskChanDataAcceptorI> channelAcceptors,
            RenderableBean renderableBean
    ) {
        this.maskAcceptors = maskAcceptors;
        this.channelAcceptors = channelAcceptors;
        this.renderableBean = renderableBean;
    }

    public void read( InputStream maskInputStream, InputStream channelStream )
            throws Exception {

        cummulativeBytesReadCount = 0;
        latestRayNumber = 0;

        // Get all the overhead stuff out of the way.
        logger.debug( "Initializing Mask Stream." );
        initializeMaskStream(maskInputStream);
        validateMaskVolume();

        logger.debug("Reading channel data.");
        List<byte[]> channelData = readChannelData( channelStream );
        logger.debug( "Completed reading channel data." );

        while ( cummulativeBytesReadCount < totalVoxels ) {
            Long skippedRayCount = readLong(maskInputStream);
            Long pairCount = readLong(maskInputStream);
            long[][] pairs = new long[ pairCount.intValue() ][ 2 ];
            for ( int i = 0; i < pairCount; i++ ) {
                pairs[ i ][ 0 ] = readLong(maskInputStream);
                pairs[ i ][ 1 ] = readLong(maskInputStream);
            }

            int nextRead = addData( skippedRayCount, pairs, channelData );
            if ( nextRead == 0 ) {
                throw new Exception("Zero bytes read.");
            }

        }

        logger.debug( "Read complete." );
    }

    //------------------------------------CONSISTENCY-CHECK METHODS
    /**
     * Returns the dimensions found for this particular
     */
    public Long[] getDimensions() {
        return new Long[] { sx, sy, sz };
    }

    /**
     * Returns the meta-data found for the incoming data.
     */
    public ChannelMetaData getChannelMetaData() {
        return channelMetaData;
    }

    //------------------------------------HELPERS
    /**
     * Add all required data to all acceptors.
     *
     * @throws Exception thrown by any called methods.
     */
    private void initializeMaskStream(InputStream maskInputStream) throws Exception {
        logger.debug( "Grabbing overhead data from mask." );

        sx = readLong(maskInputStream);
        sy = readLong(maskInputStream);
        sz = readLong(maskInputStream);

        xMicrons = readFloat(maskInputStream);
        yMicrons = readFloat(maskInputStream);
        zMicrons = readFloat(maskInputStream);

        // Reading the bounding box starts/ends.
        boundsXCoords = new Long[2];
        boundsXCoords[ 0 ] = readLong(maskInputStream);
        boundsXCoords[ 1 ] = readLong(maskInputStream);

        boundsYCoords = new Long[2];
        boundsYCoords[ 0 ] = readLong(maskInputStream);
        boundsYCoords[ 1 ] = readLong(maskInputStream);

        boundsZCoords = new Long[2];
        boundsZCoords[ 0 ] = readLong(maskInputStream);
        boundsZCoords[ 1 ] = readLong(maskInputStream);

        totalVoxels = readLong(maskInputStream);
        axis = readByte(maskInputStream);
        this.setDimensionOrder( axis );

        volumeVoxels = getVolumeVoxels( sx, sy, sz );

        for ( MaskChanDataAcceptorI acceptor: maskAcceptors ) {
            acceptor.setSpaceSize( volumeVoxels[0], volumeVoxels[1], volumeVoxels[2], coordCoverage );
        }
        for ( MaskChanDataAcceptorI acceptor: channelAcceptors ) {
            acceptor.setSpaceSize( volumeVoxels[0], volumeVoxels[1], volumeVoxels[2], coordCoverage );
        }

    }

    private void setDimensionOrder( int dimensionOrder ) {
        this.dimensionOrder = dimensionOrder;

        // Expected orderings are:  0=yz(x), 1=xz(y), 2=xy(z)
        long fastestSrcVaryingCoord;
        if ( dimensionOrder == 0 ) {
            fastestSrcVaryingMax = sx;
            secondFastestSrcVaryingMax = sy;
            slowestSrcVaryingMax = sz;

            fastestSrcVaryingCoord = 0;
            secondFastestSrcVaryingCoord = 1;
            slowestSrcVaryingCoord = 2;

        }
        else if ( dimensionOrder == 1 ) {
            fastestSrcVaryingMax = sy;
            secondFastestSrcVaryingMax = sx;
            slowestSrcVaryingMax = sz;

            fastestSrcVaryingCoord = 1;
            secondFastestSrcVaryingCoord = 0;
            slowestSrcVaryingCoord = 2;
        }
        else if ( dimensionOrder == 2 ) {
            fastestSrcVaryingMax = sz;
            secondFastestSrcVaryingMax = sx;
            slowestSrcVaryingMax = sy;

            fastestSrcVaryingCoord = 2;
            secondFastestSrcVaryingCoord = 0;
            slowestSrcVaryingCoord = 1;
        }
        else {
            throw new IllegalArgumentException( "Dimension order of " + dimensionOrder + " unexpected." );
        }

    }

    /**
     * Fetch any channel-data required for this bean.  Also, the needs of acceptors will be taken into account;
     * there may be no need to read anything here at all.
     *
     * @return list of channel arrays, raw byte data.
     * @throws Exception thrown by any called method.
     */
    private List<byte[]> readChannelData( InputStream channelStream ) throws Exception {
        List<byte[]> returnValue = new ArrayList<byte[]>();

        // Open the file, and move pointers down to seek-ready point.
        long totalIntensityVoxels = readLong( channelStream );
        if ( totalIntensityVoxels != totalVoxels ) {
            throw new IllegalArgumentException( "Mismatch in file contents: total voxels of "
                    + totalVoxels + " for mask, but total of " + totalIntensityVoxels + " for intensity/channel file."
            );
        }

        channelMetaData = new ChannelMetaData();
        channelMetaData.rawChannelCount = readByte( channelStream );
        channelMetaData.channelCount = channelMetaData.rawChannelCount;
        channelMetaData.redChannelInx = readByte( channelStream );
        channelMetaData.blueChannelInx = readByte( channelStream );
        channelMetaData.greenChannelInx = readByte( channelStream );
        channelMetaData.byteCount = readByte( channelStream );

        //  Note: any type of read requires all the mask data.  But only mask-required will necessitate
        //  the channel data all available.
        if ( channelAcceptors.size() > 0 ) {
            // NOTE: if no channels needed, the intensity stream may be ignored.

            long channelTotalBytes = totalVoxels * channelMetaData.byteCount * channelMetaData.channelCount;
            if ( channelTotalBytes > Integer.MAX_VALUE ) {
                throw new Exception( "Excessive array size encountered.  Scaling error." );
            }

            for ( MaskChanDataAcceptorI acceptor: channelAcceptors ) {
                acceptor.setChannelMetaData( channelMetaData );
            }

            // Pull in every channel's data.
            for ( int i = 0; i < channelMetaData.channelCount; i++ ) {
                byte[] nextChannelData = new byte[ totalVoxels.intValue() * channelMetaData.byteCount ];

                int bytesRead = channelStream.read(nextChannelData);
                if ( bytesRead  <  nextChannelData.length ) {
                    throw new Exception(
                            "Failed to read channel data for channel " + i + " read " + bytesRead + " bytes."
                    );
                }
                returnValue.add( nextChannelData );
            }

        }

        return returnValue;
    }

    /**
     * This is called with relative ray "coords".  Here, a ray is a multiple of the length along the fastest-varying
     * axis.  All dimensions of a rectangular solid are made up of as rays whose logical end points precede
     * the logical start points of the ones which follow, but stacked into sheets which are in turn stacked
     * into the rect-solid.  Expected orderings are:  0=yz(x), 1=xz(y), 2=xy(z).
     *
     * @param channelData available to "poke" into channel values for this renderable.
     * @param skippedRayCount tells how many of these rays to bypass before interpreting first pair.
     * @param pairsAlongRay all these pairs define interval parts of the current ray.
     * @return total bytes read during this pairs-run.
     * @throws Exception thrown by caller or if bad inputs are received.
     */
    private int addData(
            long skippedRayCount,
            long[][] pairsAlongRay,
            List<byte[]> channelData ) throws Exception {

        latestRayNumber += skippedRayCount;
        long nextRayOffset = latestRayNumber * fastestSrcVaryingMax; // No need byte-count in source coords.

        long[] srcRayStartCoords = convertTo3D( nextRayOffset );
        long[] xyzCoords = convertToStandard3D( srcRayStartCoords );  // Initialize to ray-start-pos.

        int totalPositionsAdded = 0;


        // Now, given we have dimension orderings, can leave two out of three coords in stasis, while only
        // the fastest-varying one, numbered 'axis', changes.

        long sliceSize = volumeVoxels[0] * volumeVoxels[1]; // sx * sy
        int translatedNum = renderableBean.getTranslatedNum();
        byte[] allChannelBytes = new byte[ channelMetaData.byteCount * channelMetaData.channelCount ];
        for ( long[] pairAlongRay: pairsAlongRay ) {
            for ( long rayPosition = pairAlongRay[ 0 ]; rayPosition < pairAlongRay[ 1 ]; rayPosition++ ) {
                // WARNING: The use of offsets 0,1,2 below must remain in this loop, because moving them
                // out of the loop could confound the walk-along-fastest-coord, which is not specific to any
                // particular axis, across all runs of this code.
                xyzCoords[ axis ] = rayPosition;   // Fastest-varying coord is the one walked by the pair-along-ray

                long zOffset = xyzCoords[ 2 ] * sliceSize;  // Consuming all slices to current.
                long yOffset = xyzCoords[ 1 ] * volumeVoxels[0] + zOffset;  // Consuming lines to remainder.

                long final1DCoord = yOffset + xyzCoords[ 0 ] + pairAlongRay[ 0 ];

                for ( MaskChanDataAcceptorI acceptor: maskAcceptors ) {
                    acceptor.addMaskData( translatedNum, final1DCoord );
                }

                // Here, must get the channel data.  This will include all bytes for each channel organized parallel.
                if ( channelAcceptors.size() > 0 ) {
                    for ( int i = 0; i < channelMetaData.channelCount; i++ ) {
                        byte[] nextChannelData = channelData.get( i );
                        for ( int j=0; j < channelMetaData.byteCount; j++ ) {
                            int targetOffset = (i * channelMetaData.byteCount) + j;
                            allChannelBytes[ targetOffset ] = nextChannelData[ cummulativeBytesReadCount + j ];
                        }
                    }
                    for ( MaskChanDataAcceptorI acceptor: channelAcceptors ) {
                        acceptor.addChannelData( allChannelBytes, final1DCoord );
                    }

                }
                cummulativeBytesReadCount += channelMetaData.byteCount;

            }

            long positionsReadFromPair = pairAlongRay[1] - pairAlongRay[0];
            totalPositionsAdded += positionsReadFromPair;

        }

        // Necessary to bump latest-ray, in order to move on to the "expected next" value.
        //   Here, it is assumed that if the next "addData" is called and the ray _after_
        //   this one contains non-zero voxels, a skipped ray count of 0 will be passed.
        latestRayNumber ++;

        return totalPositionsAdded;
    }

    /** Size of volume mask.  Numbers of voxels in all three directions. */
    private Long[] getVolumeVoxels( long sx, long sy, long sz ) {
        Long[] voxels = { sx, sy, sz };

        // May need to add more bytes to ensure that the coords are each multiples of a certain number of bytes.
        // If we do, we must take that into account for applying texture coordinates.
        coordCoverage = new float[] { 1.0f, 1.0f, 1.0f };

        // Axial-length-divisibilty is not necessary while using down-sampling.
        //
        //    for ( int i = 0; i < voxels.length; i++ ) {
        //        long leftover = voxels[i] % REQUIRED_AXIAL_LENGTH_DIVISIBLE;
        //        if ( leftover > 0 ) {
        //            long voxelModCount = REQUIRED_AXIAL_LENGTH_DIVISIBLE - leftover;
        //            long newVoxelCount = voxels[ i ] + voxelModCount;
        //            coordCoverage[ i ] = ((float)voxels[ i ]) / ((float)newVoxelCount);
        //            voxels[ i ] = newVoxelCount;
        //            logger.info("Expanding edge by " + voxelModCount);
        //        }
        //    }

        return voxels;
    }

    /** Call this prior to any update-data operations. */
    private void validateMaskVolume() {
        //long arrayLength = sx * sy * sz * byteCount;
        //if ( arrayLength > Integer.MAX_VALUE ) {
        //    throw new IllegalArgumentException(
        //            "Total length of input: " + arrayLength  +
        //            " exceeds maximum array size capacity.  If this is truly required, code redesign will be necessary."
        //    );
        //}

        for ( long volumeVoxel: volumeVoxels ) {
            if ( volumeVoxel > Integer.MAX_VALUE ) {
                throw new IllegalArgumentException(
                        "One or more of the axial lengths (" + sx + "," + sy + "," + sz +
                                ") exceeds max value for an integer, after padding to proper divisible size.  " +
                                "If this is truly required, code redesign will be necessary."
                );
            }
        }
        if ( sx == 0 || sy == 0 || sz == 0 ) {
            throw new IllegalArgumentException(
                    "One or more axial lengths are zero."
            );
        }

    }

    private long[] convertTo3D( long coord1DSource ) {
        // This works because the whole solid is made up of a stack of slices.
        //  ALSO, no need for byte-count in calculations for source coordinates.
        long sizeOfSlice = fastestSrcVaryingMax * secondFastestSrcVaryingMax;
        long sliceRemainder = coord1DSource % sizeOfSlice;
        long sliceNumber = coord1DSource / sizeOfSlice;   // Last slice _before_ current one.

        long sizeOfLine = fastestSrcVaryingMax;
        long lineNumber = sliceRemainder / sizeOfLine;    // Last line _before_ current one.

        // This works because the whole solid is made up of mulitples of multiples of fastest-varying-coord max.
        long pointNumber = coord1DSource % sizeOfLine;

        // After these calculations, the three-D coord of the original point in _its_ coord system is:
        //  pointNumber, lineNumber, sliceNumber
        return new long[] {
                pointNumber, lineNumber, sliceNumber
        };
    }

    /**
     * The coordinates must be moved around to represent the final axis order.
     * 0=yz(x), 1=xz(y), 2=xy(z)
     *
     * @param srcCoords ray-finding axis order from input data.
     * @return cannonical axis-ordered coordinate triple for output.
     */
    private long[] convertToStandard3D( long[] srcCoords ) {
        long[] returnVal = null;
        if ( dimensionOrder == 0 ) {
            // X,Y,Z
            returnVal = srcCoords;
        }
        else if ( dimensionOrder == 1 ) {
            // Y,X,Z
            returnVal = new long[ 3 ];
            returnVal[ 0 ] = srcCoords[ 1 ];   // File's 2nd-> X
            returnVal[ 1 ] = srcCoords[ 0 ];   // File's 1st-> Y
            returnVal[ 2 ] = srcCoords[ 2 ];   // File's 3rd-> Z
        }
        else if ( dimensionOrder == 2 ) {
            // Z,X,Y
            returnVal = new long[ 3 ];
            returnVal[ 0 ] = srcCoords[ 1 ];   // File's 2nd-> X
            returnVal[ 1 ] = srcCoords[ 2 ];   // File's 3rd-> Y
            returnVal[ 2 ] = srcCoords[ 0 ];   // File's 1st-> Z
        }
        return returnVal;
    }

    /**
     * Reads a single byte from the input stream, in LSB order.
     *
     * @param is an input stream pointing at data whose next value is a byte.
     * @return next byte from the stream.
     * @throws Exception thrown by called methods.
     */
    private byte readByte( InputStream is ) throws Exception {
        return (byte)is.read();
    }

    /**
     * Reads a single long from the input stream, in LSB order.
     *
     * @param is an input stream pointing at data whose next value is a long.
     * @return next long from the stream.
     * @throws Exception thrown by called methods, or if insufficient data remains.
     */
    private long readLong( InputStream is ) throws Exception {
        if ( is.read( longArray ) < LONG_BYTES) {
            throw new Exception( "Unexpected end of file while reading a long." );
        }
        // DEBUG
        //for ( int i = 0; i < LONG_BYTES; i++ ) {
        //    System.out.print( longArray[ i ] + " " );
        //}
        //System.out.println();
        longBuffer.rewind();

        return longBuffer.getLong();
    }

    /**
     * Reads a single float from the input stream, in LSB order.
     *
     * @param is an input stream pointing at data whose next value is a long.
     * @return next float from the stream.
     * @throws Exception thrown by called methods, or if insufficient data remains.
     */
    private float readFloat( InputStream is ) throws Exception {
        if ( is.read( floatArray ) < FLOAT_BYTES) {
            throw new Exception( "Unexpected end of file while reading a float." );
        }
        floatBuffer.rewind();

        return floatBuffer.getFloat();
    }

    /**
     * Reads a single long from the input stream, in LSB order.
     *
     * @param raf an input stream pointing at data whose next value is a long.
     * @return next long from the stream.
     * @throws Exception thrown by called methods, or if insufficient data remains.
     */
    private long readLong( RandomAccessFile raf ) throws Exception {
        if ( raf.read( longArray ) < LONG_BYTES ) {
            throw new Exception( "Unexpected end of file while reading a long." );
        }
        // DEBUG
        //for ( int i = 0; i < LONG_BYTESE; i++ ) {
        //    System.out.print( longArray[ i ] + " " );
        //}
        //System.out.println();
        longBuffer.rewind();

        return longBuffer.getLong();
    }

}
