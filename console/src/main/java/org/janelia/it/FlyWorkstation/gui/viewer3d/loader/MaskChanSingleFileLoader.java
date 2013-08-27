package org.janelia.it.FlyWorkstation.gui.viewer3d.loader;

import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
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

    public static final int REQUIRED_AXIAL_LENGTH_DIVISIBLE = 64;
    private static final int FLOAT_BYTES = Float.SIZE / 8;
    private static final int LONG_BYTES = Long.SIZE / 8;

    private static final boolean DEBUG = false;

    private long sx;
    private long sy;
    private long sz;

    private int minimumAxialDivisibility = REQUIRED_AXIAL_LENGTH_DIVISIBLE;

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
    private int intensityDivisor = 1;
    private Long totalVoxels;

    private long srcSliceSize;
    private long targetSliceSize;

    private Collection<MaskChanDataAcceptorI> maskAcceptors;
    private Collection<MaskChanDataAcceptorI> channelAcceptors;
    private RenderableBean renderableBean;

    private ByteFrequencyDumper frequencyAnalyzer;

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
    private int cummulativeVoxelsReadCount = 0;  // evolving state.

    private Logger logger = LoggerFactory.getLogger( MaskChanSingleFileLoader.class );

    private byte[] allFChannelBytes;

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

    public void setAxialLengthDivisibility( int minDivisibility ) {
        minimumAxialDivisibility = minDivisibility;
    }

    public void setIntensityDivisor(int intensityDivisor) {
        this.intensityDivisor = intensityDivisor;
    }

    /**
     * This can be called instead of the full read (below) to get only this one piece of metadata about
     * the input.
     *
     * @param maskInputStream a mask file.
     * @return its voxel count.
     * @throws Exception by called methods.
     */
    public long getVoxelCount( InputStream maskInputStream ) throws Exception {
        this.initializeMaskStream( maskInputStream );
        return totalVoxels;
    }

    /**
     * Call this to scan the full relevant data from the input files, so that info may be pushed to the
     * acceptors as it is encountered.
     *
     * @param maskInputStream points to mask data of the pair.
     * @param channelStream points to channel data of the pair.
     * @throws Exception by called methods.
     */
    public void read( InputStream maskInputStream, InputStream channelStream )
            throws Exception {

        cummulativeVoxelsReadCount = 0;
        latestRayNumber = 0;

        // Get all the overhead stuff out of the way.
        logger.debug( "Initializing Mask Stream." );

        initializeMaskStream(maskInputStream);
        validateMaskVolume();

        List<byte[]> channelData = null;
        if ( channelStream == null ) {
            logger.debug( "Creating empty channel metadata for nonexistent input stream." );
            createEmptyChannelMetaData();
        }
        else {
            logger.debug("Reading channel data.");
            channelData = readChannelData( channelStream );

            logger.debug( "Completed reading channel data." );
        }

        if ( DEBUG ) {
            frequencyAnalyzer = new ByteFrequencyDumper(
                    renderableBean.getRenderableEntity().getName() + " " + renderableBean.getLabelFileNum(),
                    channelMetaData.byteCount,
                    channelMetaData.channelCount
            );
        }

        while ( cummulativeVoxelsReadCount < totalVoxels ) {
            Long skippedRayCount = readLong(maskInputStream);
            assert saneSkipCount( skippedRayCount ) :
                    String.format( "Skipped Ray Count %d failed.\n", skippedRayCount );
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
        if ( DEBUG ) {
            frequencyAnalyzer.close();
        }
    }

    private void createEmptyChannelMetaData() {
        channelMetaData = new ChannelMetaData();
        channelMetaData.rawChannelCount = 3;
        channelMetaData.channelCount = 4;
        channelMetaData.redChannelInx = 0;
        channelMetaData.greenChannelInx = 1;
        channelMetaData.blueChannelInx = 2;
        channelMetaData.byteCount = 1;

        allFChannelBytes = new byte[ channelMetaData.channelCount * channelMetaData.byteCount ];
        allFChannelBytes[ 0 ] = 127;

        if ( channelAcceptors != null ) {
            for ( MaskChanDataAcceptorI acceptor: channelAcceptors ) {
                acceptor.setChannelMetaData( channelMetaData );
            }
        }
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

    /**
     * Check that the ray-skip count makes sense, given the bounding coordinates of the 2nd-
     * fastest-varying dimension.
     *
     * @param skippedRayCount how many rays (spans of the fastest-varying dimension) is the cursor moved?
     * @return always true, to allow multiple assertion-driven tests.
     */
    private long lastRayCount = 0;
    private boolean saneSkipCount( long skippedRayCount ) {
        if ( lastRayCount == 0 ) {
            // Skip the very first ray count.
            lastRayCount = skippedRayCount;
            return true;
        }
        lastRayCount = skippedRayCount;

        if ( skippedRayCount == 0 )
            return true;

        Long[] bounds = null;
        if ( dimensionOrder == 2 ) {
            bounds = boundsYCoords;
        }
        else {
            bounds = boundsZCoords;
        }

        long boundsWidth = bounds[1] - bounds[0];                        // How many rays does the box span?
        long skipModulo = skippedRayCount % secondFastestSrcVaryingMax;  // Leftover from possible multiple plane-wrap
        if (
                skipModulo > boundsWidth  &&
                boundsWidth < secondFastestSrcVaryingMax / 2   &&           // Conservative test.
                        Math.abs( secondFastestSrcVaryingMax - skipModulo ) > boundsWidth  // Avoid false positive wrap-before
           ) {
            logger.error(
                    String.format(
                            "With bounds %d:%d, skipped ray count of %d exceeds bounding box, and dimension order %d." +
                                    "  2nd-varying max is %d, coord is %d.",
                            bounds[0], bounds[1], skippedRayCount, dimensionOrder, secondFastestSrcVaryingMax,
                            secondFastestSrcVaryingCoord
                    )
            );
        }

        return true;

    }

    //------------------------------------HELPERS
    /**
     * Add all required data to all acceptors.
     *
     * @throws Exception thrown by any called methods.
     */
    private void initializeMaskStream(InputStream maskInputStream) throws Exception {
        if ( totalVoxels == null ) {
            logger.debug( "Grabbing overhead data from mask." );

            sx = readLong(maskInputStream);
            sy = readLong(maskInputStream);
            sz = readLong(maskInputStream);
            logger.info( "Input Dimensions of {} x {} x " + sz, sx, sy );

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
            logger.debug("Total voxels={}.  Combined vol size={}.", totalVoxels, sx*sy*sz);
            axis = readByte(maskInputStream);
            logger.debug( "Got axis key of {}", axis );
            this.setDimensionOrder( axis );

            volumeVoxels = getVolumeVoxels( sx, sy, sz );

            targetSliceSize = volumeVoxels[0] * volumeVoxels[1]; // sx * sy

            srcSliceSize = fastestSrcVaryingMax * secondFastestSrcVaryingMax;

            if ( maskAcceptors != null ) {
                for ( MaskChanDataAcceptorI acceptor: maskAcceptors ) {
                    acceptor.setSpaceSize( sx, sy, sz, volumeVoxels[0], volumeVoxels[1], volumeVoxels[2], coordCoverage );
                }
            }
            if ( channelAcceptors != null ) {
                for ( MaskChanDataAcceptorI acceptor: channelAcceptors ) {
                    acceptor.setSpaceSize( sx, sy, sz, volumeVoxels[0], volumeVoxels[1], volumeVoxels[2], coordCoverage );
                }
            }
        }
    }

    private void setDimensionOrder( int dimensionOrder ) {
        this.dimensionOrder = dimensionOrder;

        // Expected orderings are:  0=yz(x), 1=xz(y), 2=xy(z)
        if ( dimensionOrder == 0 ) {
            fastestSrcVaryingMax = sx;
            secondFastestSrcVaryingMax = sz;
            slowestSrcVaryingMax = sy;

            secondFastestSrcVaryingCoord = 2;
            slowestSrcVaryingCoord = 1;
        }
        else if ( dimensionOrder == 1 ) {
            fastestSrcVaryingMax = sy;
            secondFastestSrcVaryingMax = sz;
            slowestSrcVaryingMax = sx;

            secondFastestSrcVaryingCoord = 2;
            slowestSrcVaryingCoord = 0;
        }
        else if ( dimensionOrder == 2 ) {
            fastestSrcVaryingMax = sz;
            secondFastestSrcVaryingMax = sy;
            slowestSrcVaryingMax = sx;

            secondFastestSrcVaryingCoord = 1;
            slowestSrcVaryingCoord = 0;
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
        long totalIntensityVoxels = readLong(channelStream);
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
        channelMetaData.renderableBean = this.renderableBean;

        //  Note: any type of read requires all the mask data.  But only channel-required will necessitate
        //  the channel data all be available.
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

        long[] srcRayStartCoords = convertToSrc3D( nextRayOffset );
        long[] xyzCoords = convertToStandard3D( srcRayStartCoords );  // Initialize to ray-start-pos.

        int totalPositionsAdded = 0;


        // Now, given we have dimension orderings, can leave two out of three coords in stasis, while only
        // the fastest-varying one, numbered 'axis', changes.

        int translatedNum = renderableBean.getTranslatedNum();
        byte[] allChannelBytes = new byte[ channelMetaData.byteCount * channelMetaData.channelCount ];
        for ( long[] pairAlongRay: pairsAlongRay ) {
            for ( long rayPosition = pairAlongRay[ 0 ]; rayPosition < pairAlongRay[ 1 ]; rayPosition++ ) {
                // WARNING: The use of offsets 0,1,2 below must remain in this loop, because moving them
                // out of the loop could confound the walk-along-fastest-coord, which is not specific to any
                // particular axis, across all runs of this code.

                xyzCoords[ axis ] = rayPosition;   // Fastest-varying coord is the one walked by the pair-along-ray

                long finalYCoord = xyzCoords[ 1 ];
                if ( renderableBean.isInvertedY() ) {
                    finalYCoord = sy - finalYCoord - 1;
                }

                long zOffset = xyzCoords[ 2 ] * targetSliceSize;  // Consuming all slices to current.
                long yOffset = finalYCoord * volumeVoxels[0] + zOffset;  // Consuming lines to remainder.

                long final1DCoord = yOffset + xyzCoords[ 0 ];

                for ( MaskChanDataAcceptorI acceptor: maskAcceptors ) {
                    acceptor.addMaskData( translatedNum, final1DCoord, xyzCoords[ 0 ], xyzCoords[ 1 ], xyzCoords[ 2 ] );
                }

                // Here, must get the channel data.  This will include all bytes for each channel organized parallel.
                if ( channelAcceptors.size() > 0 ) {
                    int voxelBytesAlreadyRead = cummulativeVoxelsReadCount * channelMetaData.byteCount;
                    for ( int i = 0; i < channelMetaData.channelCount; i++ ) {
                        int channelOffset = (i * channelMetaData.byteCount) + channelMetaData.byteCount - 1;
                        if ( channelData != null ) {
                            byte[] nextChannelData = channelData.get( i );
                            for ( int j=0; j < channelMetaData.byteCount; j++ ) {
                                //                                                   REVERSING byte order for Java
                                int targetOffset = channelOffset - j;
                                allChannelBytes[ targetOffset ] = (byte)(nextChannelData[ voxelBytesAlreadyRead + j ] / intensityDivisor);
                            }
                        }
                        else {
                            allChannelBytes = allFChannelBytes;
                        }
                    }
                    for ( MaskChanDataAcceptorI acceptor: channelAcceptors ) {
                        acceptor.addChannelData(
                                allChannelBytes, final1DCoord, xyzCoords[ 0 ], finalYCoord, xyzCoords[ 2 ],
                                channelMetaData
                        );
                    }

                }
                cummulativeVoxelsReadCount++;

            }

            long positionsReadFromPair = pairAlongRay[1] - pairAlongRay[0];
            totalPositionsAdded += positionsReadFromPair;

        }

        if ( DEBUG ) {
            frequencyAnalyzer.frequencyCapture( allChannelBytes );
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

        for ( int i = 0; i < voxels.length; i++ ) {
            long leftover = voxels[i] % minimumAxialDivisibility;
            if ( leftover > 0 ) {
                long voxelModCount = minimumAxialDivisibility - leftover;
                long newVoxelCount = voxels[ i ] + voxelModCount;
                coordCoverage[ i ] = ((float)voxels[ i ]) / ((float)newVoxelCount);
                voxels[ i ] = newVoxelCount;
                logger.debug("Expanding edge by {} to {}.", voxelModCount, voxels[ i ] );
            }
        }

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

    private long[] convertToSrc3D(long coord1DSource) {
        // This works because the whole solid is made up of a stack of slices.
        //  ALSO, no need for byte-count in calculations for source coordinates.
        long sliceRemainder = coord1DSource % srcSliceSize;
        long sliceNumber = coord1DSource / srcSliceSize;   // Last slice _before_ current one.

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
        long[] returnVal = new long[ 3 ];
        // Expected orderings are:  0=yz(x), 1=xz(y), 2=xy(z)
        if ( dimensionOrder == 0 ) {
            // 0=yz(x)
            returnVal[ 0 ] = srcCoords[ 0 ];
            returnVal[ 1 ] = srcCoords[ 2 ];
            returnVal[ 2 ] = srcCoords[ 1 ];
        }
        else if ( dimensionOrder == 1 ) {
            // 1=xz(y)
            returnVal[ 0 ] = srcCoords[ 2 ];
            returnVal[ 1 ] = srcCoords[ 0 ];
            returnVal[ 2 ] = srcCoords[ 1 ];
        }
        else if ( dimensionOrder == 2 ) {
            // 2=xy(z)
            returnVal[ 0 ] = srcCoords[ 2 ];   // File's 3rd-> X
            returnVal[ 1 ] = srcCoords[ 1 ];   // File's 2nd-> Y
            returnVal[ 2 ] = srcCoords[ 0 ];   // File's 1st-> Z
        }
        else {
            throw new IllegalArgumentException( "Unknown dimension order constant " + dimensionOrder );
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
