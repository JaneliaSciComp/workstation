package org.janelia.it.workstation.gui.alignment_board.loader;

import org.janelia.it.workstation.gui.alignment_board_viewer.masking.FileStats;
import org.janelia.it.workstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.workstation.shared.annotations.NotThreadSafe;
import org.janelia.it.jacs.shared.img_3d_loader.ByteFrequencyDumper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
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
public class MaskSingleFileLoader {

    public static final int REQUIRED_AXIAL_LENGTH_DIVISIBLE = 64;
    public static final int UNSET_SEGMENT = -1;

    private static final int FLOAT_BYTES = Float.SIZE / 8;
    private static final int LONG_BYTES = Long.SIZE / 8;

    private static final boolean DEBUG = false;
    public static final int SUBSTITUTE_CHANNEL_VALUE = 127;

    private long sx;
    private long sy;
    private long sz;

    private Long applicable1DStart;
    private Long applicable1DEnd;

    private int segment = UNSET_SEGMENT;
    private int numSegments = UNSET_SEGMENT;

    private int minimumAxialDivisibility = REQUIRED_AXIAL_LENGTH_DIVISIBLE;

    private Long[] volumeVoxels;
    private float[] coordCoverage;

    // These values are kept here for future reference.
    //  These bounds tell the extent of the overall space, that the current renderable occupies.
    private Long[] boundsXCoords;
    private Long[] boundsYCoords;
    private Long[] boundsZCoords;
    //  These "microns" tell the extent of real-world space occupied by a single 3D point (or voxel).
    @SuppressWarnings("unused")
    private float xMicrons;
    @SuppressWarnings("unused")
    private float yMicrons;
    @SuppressWarnings("unused")
    private float zMicrons;

    private long fastestSrcVaryingMax;
    private long secondFastestSrcVaryingMax;
    private long secondFastestSrcVaryingCoord;
    @SuppressWarnings("unused")
    private long slowestSrcVaryingMax;
    @SuppressWarnings("unused")
    private long slowestSrcVaryingCoord;

    private Byte axis;

    private ChannelMetaData channelMetaData;

    private int dimensionOrder = -1;
    private int intensityDivisor = 1;
    private Long totalVoxels;

    private long srcSliceSize;
    private long targetSliceSize;

    private Collection<MaskChanDataAcceptorI> maskAcceptors;
    private MaskChanDataAcceptorI loneMaskAcceptor;
    private Collection<MaskChanDataAcceptorI> channelAcceptors;
    private MaskChanDataAcceptorI loneChannelAcceptor;
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
    private long lastRayCount = 0;      // evolving state.

    private final Logger logger = LoggerFactory.getLogger( MaskSingleFileLoader.class );

    private byte[] allFChannelBytes;
    private double[] channelAverages;
    private FileStats fileStats;

    /**
     * Mostly-null. Only for getting voxel count into bean. Provide the bean so that the voxel sizing may be added to it.
     * @param bean store voxel count into this.
     */
    public MaskSingleFileLoader( RenderableBean bean ) {
        this(null, null, bean, null);
    }

    /**
     * Construct a file loader for all data about a single renderable, and with all targets for that data.
     *
     * @param maskAcceptors these care about mask data per se.
     * @param channelAcceptors this care about the channel data to which mask data refers.
     * @param renderableBean all actions taken here are concerning this renderable.
     */
    public MaskSingleFileLoader(
            Collection<MaskChanDataAcceptorI> maskAcceptors,
            Collection<MaskChanDataAcceptorI> channelAcceptors,
            RenderableBean renderableBean,
            FileStats fileStats
    ) {
        this.maskAcceptors = maskAcceptors;
        this.channelAcceptors = channelAcceptors;

        if ( maskAcceptors != null  &&  maskAcceptors.size() == 1 ) {
            loneMaskAcceptor = maskAcceptors.iterator().next();
        }
        else {
            this.maskAcceptors = Collections.EMPTY_LIST;
        }

        if ( channelAcceptors != null  &&  channelAcceptors.size() == 1 ) {
            loneChannelAcceptor = channelAcceptors.iterator().next();
        }
        else {
            this.channelAcceptors = Collections.EMPTY_LIST;
        }

        this.renderableBean = renderableBean;
        this.fileStats = fileStats;
    }

    public void setAxialLengthDivisibility( int minDivisibility ) {
        minimumAxialDivisibility = minDivisibility;
    }

    public void setIntensityDivisor(int intensityDivisor) {
        this.intensityDivisor = intensityDivisor;
    }

    /** Seed with info used to establish 1D array boundaries for this loader. */
    public void setApplicableSegment( int segment, int numSegments ) {
        this.segment = segment;
        this.numSegments = numSegments;
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
     * @param channelDataBean points to channel data of the pair.
     * @throws Exception by called methods.
     */
    @NotThreadSafe(why = "calls addData")
    public void read( InputStream maskInputStream, ChannelSingleFileLoader.ChannelDataBean channelDataBean )
            throws Exception {

        cummulativeVoxelsReadCount = 0;
        latestRayNumber = 0;

        // Get all the overhead stuff out of the way.
        logger.debug( "Initializing Mask Stream." );

        initializeMaskStream(maskInputStream);
        validateMaskVolume();

        List<byte[]> channelIntensityBytes = null;
        if ( channelDataBean == null || channelDataBean.getChannelData() == null ) {
            logger.debug( "Creating empty channel metadata for nonexistent input stream." );
            createEmptyChannelMetaData();
        }
        else {
            channelIntensityBytes = channelDataBean.getChannelData();
            channelMetaData = channelDataBean.getChannelMetaData();
            pushChannelMetaDataToAcceptors();
        }

        if ( DEBUG ) {
            frequencyAnalyzer = new ByteFrequencyDumper(
                    renderableBean.getName() + " " + renderableBean.getLabelFileNum(),
                    channelMetaData.byteCount,
                    channelMetaData.channelCount
            );
        }

        channelAverages = new double[ channelMetaData.rawChannelCount ];

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

            int nextRead = addData( skippedRayCount, pairs, channelIntensityBytes );
            if ( nextRead == 0 ) {
                throw new Exception("Zero bytes read.");
            }

        }

        // Channel only available if presence of acceptors signalled its read.
        if ( channelAcceptors.size() > 0  &&  renderableBean.getItem() != null  &&  fileStats != null ) {
            if ( logger.isDebugEnabled() ) {
                StringBuilder averages = new StringBuilder( "Average values for " )
                        .append( renderableBean.getName() ).append( " at total voxels of " )
                        .append( renderableBean.getVoxelCount() ).append( " are:\n" );
                for ( int i = 0; i < channelAverages.length; i++ ) {
                    averages.append( "Channel ").append( i ).append(' ').append( channelAverages[ i ] ).append( "\n" );
                }
                logger.debug( averages.toString() );
            }

            fileStats.recordChannelAverages( renderableBean.getId(), channelAverages );
        }
        else if ( channelAcceptors.size() > 0 ) {
            logger.warn(
                    "No color averages recorded.  Renderable bean: entity={}.  Filestats={}.",
                    renderableBean.getId(), fileStats + " " + channelAcceptors.size()
            );
        }

        logger.debug( "Read complete." );
        if ( DEBUG ) {
            frequencyAnalyzer.close();
        }
    }

    private void pushChannelMetaDataToAcceptors() {
        if ( channelAcceptors != null ) {
            for ( MaskChanDataAcceptorI acceptor: channelAcceptors ) {
                acceptor.setChannelMetaData( channelMetaData );
            }
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
        for ( int i = 0; i < allFChannelBytes.length; i+=channelMetaData.byteCount ) {
            allFChannelBytes[ i ] = SUBSTITUTE_CHANNEL_VALUE;
        }

        pushChannelMetaDataToAcceptors();
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
    private boolean saneSkipCount( long skippedRayCount ) {
        if ( lastRayCount == 0 ) {
            // Skip the very first ray count.
            lastRayCount = skippedRayCount;
            return true;
        }
        lastRayCount = skippedRayCount;

        if ( skippedRayCount == 0 )
            return true;

        Long[] bounds;
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
            logger.debug( "Input Dimensions of {} x {} x " + sz, sx, sy );

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
            if ( renderableBean != null ) {
                renderableBean.setVoxelCount( totalVoxels );
            }
            logger.debug("Total voxels={}.  Combined vol size={}.", totalVoxels, sx*sy*sz);
            axis = readByte(maskInputStream);
            logger.debug( "Got axis key of {}", axis );
            this.setDimensionOrder( axis );

            volumeVoxels = getVolumeVoxels( sx, sy, sz );

            targetSliceSize = volumeVoxels[0] * volumeVoxels[1]; // sx * sy

            srcSliceSize = fastestSrcVaryingMax * secondFastestSrcVaryingMax;

            // Establish applicable boundaries for this loader, if any were expected.
            //  These are for multi-thread support, and mustn't be confused with the bounding box.
            if ( segment != UNSET_SEGMENT ) {
                long total1D = sx * sy * sz;
                long segmentSize = total1D / numSegments;
                applicable1DStart = segmentSize * segment;
                applicable1DEnd = applicable1DStart + segmentSize;
                if ( applicable1DEnd > total1D ) {
                    applicable1DEnd = total1D;
                }
                if (logger.isTraceEnabled()) {
                    if ( renderableBean != null ) {
                        logger.trace( "Single file loader looking at positions {} to {} for bean {}, t-num {}.", new Object[] {applicable1DStart, applicable1DEnd, renderableBean.getName(), renderableBean.getTranslatedNum() } );
                    }
                }
            }

            if ( loneMaskAcceptor != null ) {
                loneMaskAcceptor.setSpaceSize( sx, sy, sz, volumeVoxels[0], volumeVoxels[1], volumeVoxels[2], coordCoverage );
            }
            else {
                for ( MaskChanDataAcceptorI acceptor: maskAcceptors ) {
                    acceptor.setSpaceSize( sx, sy, sz, volumeVoxels[0], volumeVoxels[1], volumeVoxels[2], coordCoverage );
                }
            }

            if ( loneChannelAcceptor != null ) {
                loneChannelAcceptor.setSpaceSize( sx, sy, sz, volumeVoxels[0], volumeVoxels[1], volumeVoxels[2], coordCoverage );
            }
            else {
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
            throw new IllegalArgumentException( "Dimension order of " + dimensionOrder + " unexpected. " + renderableBean.getName() );
        }

    }

    /**
     * This is called with relative ray "coords".  Here, a ray is a multiple of the length along the fastest-varying
     * axis.  All dimensions of a rectangular solid are made up of as rays whose logical end points precede
     * the logical start points of the ones which follow, but stacked into sheets which are in turn stacked
     * into the rect-solid.  Expected orderings are:  0=yz(x), 1=xz(y), 2=xy(z).
     *
     * @param channelIntensityBytes available to "poke" into channel values for this renderable.
     * @param skippedRayCount tells how many of these rays to bypass before interpreting first pair.
     * @param pairsAlongRay all these pairs define interval parts of the current ray.
     * @return total bytes read during this pairs-run.
     * @throws Exception thrown by caller or if bad inputs are received.
     */
    @NotThreadSafe(why="Calls acceptors' addChannelData/addMaskData; updates latestRayNumber")
    private int addData(
            long skippedRayCount,
            long[][] pairsAlongRay,
            final List<byte[]> channelIntensityBytes ) throws Exception {

        latestRayNumber += skippedRayCount;
        long nextRayOffset = latestRayNumber * fastestSrcVaryingMax; // No need byte-count in source coords.

        long[] srcRayStartCoords = convertToSrc3D( nextRayOffset );
        final long[] xyzCoords = convertToStandard3D( srcRayStartCoords );  // Initialize to ray-start-pos.

        int totalPositionsAdded = 0;

        // Now, given we have dimension orderings, can leave two out of three coords in stasis, while only
        // the fastest-varying one, numbered 'axis', changes.

        final int translatedNum = renderableBean.getTranslatedNum();
        final double totalVoxelFactor = 1.0 / ( (double)totalVoxels * Math.pow( 256, channelMetaData.byteCount ) );
        final byte[] allChannelBytes = new byte[ channelMetaData.byteCount * channelMetaData.channelCount ];

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

                final long fixedFinalYCoord = finalYCoord;

                long zOffset = xyzCoords[ 2 ] * targetSliceSize;  // Consuming all slices to current.
                long yOffset = finalYCoord * volumeVoxels[0] + zOffset;  // Consuming lines to remainder.

                final long final1DCoord = yOffset + xyzCoords[ 0 ];
                if ( applicable1DStart == null  ||
                     ( final1DCoord >= applicable1DStart  &&  final1DCoord < applicable1DEnd ) ) {
                    writeToMaskAcceptors(xyzCoords, translatedNum, finalYCoord, final1DCoord);
                    writeToChannelAcceptors(channelIntensityBytes, xyzCoords, translatedNum, totalVoxelFactor, allChannelBytes, fixedFinalYCoord, final1DCoord);
                    if ( DEBUG )
                        frequencyAnalyzer.frequencyCapture( allChannelBytes );
                }

                cummulativeVoxelsReadCount++;

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

    private void writeToMaskAcceptors(long[] xyzCoords, int translatedNum, long finalYCoord, long final1DCoord) throws Exception {
        if ( loneMaskAcceptor != null ) {
            loneMaskAcceptor.addMaskData( translatedNum, final1DCoord, xyzCoords[ 0 ], finalYCoord, xyzCoords[ 2 ] );
        }
        else {
            for ( MaskChanDataAcceptorI acceptor: maskAcceptors ) {
                acceptor.addMaskData( translatedNum, final1DCoord, xyzCoords[ 0 ], finalYCoord, xyzCoords[ 2 ] );
            }
        }
    }

    private byte[] writeToChannelAcceptors(List<byte[]> channelData, long[] xyzCoords, int translatedNum, double totalVoxelFactor, byte[] allChannelBytes, long finalYCoord, long final1DCoord) throws Exception {
        // Here, must get the channel data.  This will include all bytes for each channel organized parallel.
        if ( channelAcceptors.size() > 0 ) {
            int voxelBytesAlreadyRead = cummulativeVoxelsReadCount * channelMetaData.byteCount;
            int[] orderedRgbIndexes = channelMetaData.getOrderedRgbIndexes();
            for ( int iChnl = 0; iChnl < channelMetaData.channelCount; iChnl++ ) {
                int channelValue = 0;
                int channelOffset = (iChnl * channelMetaData.byteCount) + channelMetaData.byteCount - 1;
                if ( channelData != null ) {
                    byte[] nextChannelData = channelData.get( iChnl );
                    for ( int iByte = 0; iByte < channelMetaData.byteCount; iByte++ ) {
                        //                                                   REVERSING byte order for Java
                        int targetOffset = channelOffset - iByte;
                        int nextChannelValue = nextChannelData[voxelBytesAlreadyRead + iByte];
                        if ( nextChannelValue < 0 ) {
                            nextChannelValue += 256;
                        }
                        allChannelBytes[ targetOffset ] = (byte)(nextChannelValue / intensityDivisor);

                        // Save full channel value for statistical calculations.
                        channelValue += nextChannelValue
                                     << 8 * (channelMetaData.byteCount - iByte - 1);
                    }

                    channelAverages[ orderedRgbIndexes[ iChnl ] ] += channelValue * totalVoxelFactor;
                }
                else {
                    allChannelBytes = new byte[ allFChannelBytes.length ];
                    for ( int aci = 0; aci < allFChannelBytes.length; aci++ ) {
                        allChannelBytes[ aci ] = (byte)(allFChannelBytes[ aci ] / intensityDivisor);
                    }
                }
            }
            if ( loneChannelAcceptor != null ) {
                loneChannelAcceptor.addChannelData(
                        translatedNum,
                        allChannelBytes, final1DCoord, xyzCoords[ 0 ], finalYCoord, xyzCoords[ 2 ],
                        channelMetaData
                );
            }
            else {
                for ( MaskChanDataAcceptorI acceptor: channelAcceptors ) {
                    acceptor.addChannelData(
                            translatedNum,
                            allChannelBytes, final1DCoord, xyzCoords[ 0 ], finalYCoord, xyzCoords[ 2 ],
                            channelMetaData
                    );
                }
            }

        }
        return allChannelBytes;
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

    /*
      Format for mask files.
      Mask files:
    long xsize; // space
    long ysize; // space
    long zsize; // space
    long x0; // bounding box
    long x1; // bounding box, such that x0 is inclusive, x1 exclusive, etc
    long y0; // bb
    long y1; // bb
    long z0; // bb
    long z1; // bb
    long totalVoxels;
    unsigned char axis; // 0=yz(x), 1=xz(y), 2=xy(z)
    { // For each ray
      long skip;
      long pairs;
      { // For each pair
        long start;
        long end; // such that end-start is length, i.e., end is exclusive
      }
    }
    */

}
