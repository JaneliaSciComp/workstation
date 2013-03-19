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

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/13/13
 * Time: 4:21 PM
 *
 * This implementation of a mask builder takes renderables as its driving data.  It will accept the renderables,
 * along with their applicable chunks of data, to produce its texture data volume, in memory.
 */
public class MaskChanFileLoader {

    private static final int FLOAT_BYTES = Float.SIZE / 8;
    private static final int LONG_BYTES = Long.SIZE / 8;
    private static final int START_OF_RAW_CHANNELS = LONG_BYTES + 5;
    private long sx;
    private long sy;
    private long sz;

    //  These "microns" tell the extent of real-world space occupied by a single 3D point (or voxel).
    private float xMicrons;
    private float yMicrons;
    private float zMicrons;

    private long fastestSrcVaryingMax;
    private long secondFastestSrcVaryingMax;
    private long secondFastestSrcVaryingCoord;
    private long slowestSrcVaryingMax;
    private long slowestSrcVaryingCoord;

    private Long[] boundsXCoords;
    private Long[] boundsYCoords;
    private Long[] boundsZCoords;
    private Byte axis;

    private ChannelMetaData channelMetaData;


    //private Collection<RenderableBean> renderableBeans;
    private int byteCount = 0;
    private int dimensionOrder = -1;
    private Long totalVoxels;
    private Long channelTotalBytes;

    private Collection<MaskChanDataAcceptorI> maskAcceptors;
    private Collection<MaskChanDataAcceptorI> channelAcceptors;

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

    private Logger logger = LoggerFactory.getLogger( MaskChanFileLoader.class );

    public void setByteCount( int byteCount ) {
        this.byteCount = byteCount;
    }

    public void setDimensionOrder( int dimensionOrder ) {
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

    /** Anything on this list could receive data from the files under study. */
    public void setAcceptors( Collection<MaskChanDataAcceptorI> acceptors ) {
        maskAcceptors = new ArrayList<MaskChanDataAcceptorI>();
        channelAcceptors = new ArrayList<MaskChanDataAcceptorI>();

        for ( MaskChanDataAcceptorI acceptor: acceptors ) {
            if ( acceptor.getAcceptableInputs().equals( MaskChanDataAcceptorI.Acceptable.channel ) ) {
                channelAcceptors.add( acceptor );
            }
            if ( acceptor.getAcceptableInputs().equals( MaskChanDataAcceptorI.Acceptable.mask ) ) {
                maskAcceptors.add( acceptor );
            }
            if ( acceptor.getAcceptableInputs().equals( MaskChanDataAcceptorI.Acceptable.both ) ) {
                channelAcceptors.add( acceptor );
                maskAcceptors.add( acceptor );
            }
        }
    }

    public void read( RenderableBean bean, InputStream maskInputStream ) throws Exception {
        read( bean, maskInputStream, null );
    }

    public void read( RenderableBean bean, InputStream maskInputStream, RandomAccessFile channelRAF )
            throws Exception {
        logger.info("Read called.");

        long totalRead = 0;

        // Get all the overhead stuff out of the way.
        initializeStreams( maskInputStream, channelRAF );
        validateMaskVolume();

        while ( totalRead < totalVoxels ) {
            Long skippedRayCount = readLong(maskInputStream);
            Long pairCount = readLong(maskInputStream);
            long[][] pairs = new long[ pairCount.intValue() ][ 2 ];
            for ( int i = 0; i < pairCount; i++ ) {
                pairs[ i ][ 0 ] = readLong(maskInputStream);
                pairs[ i ][ 1 ] = readLong(maskInputStream);
            }

            int nextRead = addData( bean, skippedRayCount, pairs );

            totalRead += nextRead;
            if ( nextRead == 0 ) {
                throw new Exception("Zero bytes read.");
            }

        }

    }

    /**
     * Add all required data to all acceptors.
     *
     * @throws Exception thrown by any called methods.
     */
    private void initializeStreams( InputStream maskInputStream, RandomAccessFile channelRAF ) throws Exception {
        logger.info( "Grabbing overhead data from mask." );

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

        //  Note: any type of read requires all the mask data.  But only mask-required will require
        //  the channel data all available.
        if ( channelAcceptors.size() > 0 ) {
            // NOTE: if no channels needed, the intensity stream may be ignored.
            // Open the file, and move pointers down to seek-ready point.
            long totalIntensityVoxels = readLong( channelRAF );
            if ( totalIntensityVoxels != totalVoxels ) {
                throw new IllegalArgumentException( "Mismatch in file contents: total voxels of "
                    + totalVoxels + " for mask, but total of " + totalIntensityVoxels + " for intensity/channel file."
                );
            }

            channelMetaData = new ChannelMetaData();
            channelMetaData.channelCount = readByte( channelRAF );
            channelMetaData.redChannelInx = readByte( channelRAF );
            channelMetaData.blueChannelInx = readByte( channelRAF );
            channelMetaData.greenChannelInx = readByte( channelRAF );
            channelMetaData.byteCount = readByte( channelRAF );

            channelTotalBytes = totalVoxels * channelMetaData.byteCount * channelMetaData.channelCount;
        }

        for ( MaskChanDataAcceptorI acceptor: maskAcceptors ) {
            acceptor.setSpaceSize( sx, sy, sz );
        }
        for ( MaskChanDataAcceptorI acceptor: channelAcceptors ) {
            acceptor.setSpaceSize( sx, sy, sz );
        }

    }

    /**
     * This is called with relative ray "coords".  Here, a ray is a multiple of the length along the fastest-varying
     * axis.  All dimensions of a rectangular solid are made up of as rays whose logical end points precede
     * the logical start points of the ones which follow, but stacked into sheets which are in turn stacked
     * into the rect-solid.  Expected orderings are:  0=yz(x), 1=xz(y), 2=xy(z).
     *
     * @param renderable describes all points belonging to all pairs.
     * @param skippedRayCount tells how many of these rays to bypass before interpreting first pair.
     * @param pairsAlongRay all these pairs define interval parts of the current ray.
     * @return total bytes read during this pairs-run.
     * @throws Exception thrown by caller or if bad inputs are received.
     */
    private int addData( RenderableBean renderable, long skippedRayCount, long[][] pairsAlongRay ) throws Exception {
        latestRayNumber += skippedRayCount;
        long nextRayOffset = latestRayNumber * fastestSrcVaryingMax;

        long[] srcRayStartCoords = convertTo3D( nextRayOffset, fastestSrcVaryingMax, secondFastestSrcVaryingMax );
        long[] xyzCoords = convertToStandard3D( srcRayStartCoords );  // Initialize to ray-start-pos.

        int totalBytesRead = 0;

        // Now, given we have dimension orderings, can leave two out of three coords in stasis, while only
        // the fastest-varying one, numbered 'axis', changes.
        long sliceSize = sx * sy;
        int translatedNum = renderable.getTranslatedNum();
        for ( long[] pairAlongRay: pairsAlongRay ) {
            for ( long rayPosition = pairAlongRay[ 0 ]; rayPosition < pairAlongRay[ 1 ]; rayPosition++ ) {
                // WARNING: The use of offsets 0,1,2 below must remain in this loop, because moving them
                // out of the loop could confound the walk-along-fastest-coord, which is not specific to any
                // particular axis, across all runs of this code.
                xyzCoords[ axis ] = rayPosition;   // Fastest-varying coord is the one walked by the pair-along-ray

                long zOffset = xyzCoords[ 2 ] * sliceSize;  // Consuming all slices to current.
                long yOffset = xyzCoords[ 1 ] * sx + zOffset;  // Consuming lines to remainder.

                long final1DCoord = yOffset + xyzCoords[ 0 ] + pairAlongRay[ 0 ];

                for ( MaskChanDataAcceptorI acceptor: maskAcceptors ) {
                    acceptor.addMaskData( translatedNum, final1DCoord );
                }

                // Here, must go to the channel data.
                if ( channelAcceptors.size() > 0 ) {

                    // TODO, push the channel data in from caller of "addData"
                    // Fetch THIS channel data, and store it into the acceptor.
                    byte[] channelData = new byte[ channelMetaData.channelCount * channelMetaData.byteCount ];
                    int nextArrPos = 0;
                    int inFilePos = START_OF_RAW_CHANNELS + (totalBytesRead * channelMetaData.byteCount );

                    // NEED to seek the RAF to the right position, read in bytes to fill channel data, and copy
                    //  that into the acceptor.

                    for ( MaskChanDataAcceptorI acceptor: channelAcceptors ) {
                        acceptor.addChannelData( channelData, final1DCoord );
                    }
                }

            }

            totalBytesRead += pairAlongRay[ 1 ] - pairAlongRay[ 0 ];

        }

        // Necessary to bump latest-ray, in order to move on to the "expected next" value.
        //   Here, it is assumed that if the next "addData" is called and the ray _after_
        //   this one contains non-zero voxels, a skipped ray count of 0 will be passed.
        latestRayNumber ++;

        return totalBytesRead;
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

        if ( sx > Integer.MAX_VALUE || sy > Integer.MAX_VALUE || sz > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException(
                    "One or more of the axial lengths (" + sx + "," + sy + "," + sz +
                    ") exceeds max value for an integer.  If this is truly required, code redesign will be necessary."
            );
        }
        else if ( sx == 0 || sy == 0 || sz == 0 ) {
            throw new IllegalArgumentException(
                    "One or more axial lengths are zero."
            );
        }

    }

    private long[] convertTo3D( long coord1DSource, long fastestSrcVaryingMax, long nextFastestSrcVaryingMax ) {
        // This works because the whole solid is made up of a stack of slices.
        long sizeOfSlice = fastestSrcVaryingMax * nextFastestSrcVaryingMax;
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
     * Reads a single byte from the input stream, in LSB order.
     *
     * @param raf a random access file, being read at current file pointer.
     * @return next byte from the stream.
     * @throws Exception thrown by called methods.
     */
    private byte readByte( RandomAccessFile raf ) throws Exception {
        return (byte)raf.readUnsignedByte();
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

    public static class ChannelMetaData {
        public int byteCount = -1;
        public int channelCount = -1;
        public int blueChannelInx = -1;
        public int greenChannelInx = -1;
        public int redChannelInx = -1;
    }
}
