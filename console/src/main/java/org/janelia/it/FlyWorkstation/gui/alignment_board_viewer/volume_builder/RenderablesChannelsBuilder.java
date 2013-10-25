package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardSettings;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.MultiTexVolumeBrick;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.ChannelMetaData;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.VolumeLoaderI;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.*;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.VolumeDataI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.volume_builder.VolumeDataChunk;
import org.janelia.it.FlyWorkstation.shared.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL2;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/13/13
 * Time: 4:21 PM
 *
 * This implementation of a mask builder takes renderables as its driving data.  It will accept the renderables,
 * along with their applicable chunks of data, to produce its texture data volume, in memory.
 */
public class RenderablesChannelsBuilder extends RenderablesVolumeBuilder implements VolumeLoaderI, TextureBuilderI {

    private static final String COUNT_DISCREPANCY_FORMAT = "%s count mismatch. Old count was %d; new count is %d.\n";

    private static final int FIXED_BYTE_PER_CHANNEL = 1;
    protected int bytesPerChannel = FIXED_BYTE_PER_CHANNEL;

    private ChannelMetaData channelMetaData;
    private VolumeDataI channelVolumeData;

    private ChannelInterpreterI channelInterpreter;
    private final AlignmentBoardSettings settings;
    private final Collection<RenderableBean> renderableBeans;
    private VolumeDataI maskVolumeData;
    private MultiMaskTracker multiMaskTracker;

    protected boolean needsChannelInit = false; // Initialized for emphasis.
    private final Logger logger = LoggerFactory.getLogger( RenderablesChannelsBuilder.class );

    public RenderablesChannelsBuilder(
            AlignmentBoardSettings settings,
            MultiMaskTracker multiMaskTracker,
            VolumeDataI maskVolumeData,
            Collection<RenderableBean> renderableBeans
    ) {
        super();  // ...and I _mean_ that!
        needsChannelInit = true; // Must initialize the channel-specific data.
        this.settings = settings;
        this.multiMaskTracker = multiMaskTracker;
        this.renderableBeans = renderableBeans;
        this.maskVolumeData = maskVolumeData;

        channelMetaData = new ChannelMetaData();
        channelMetaData.rawChannelCount = 3; // Forcing a good upper bound.
        channelMetaData.channelCount = 4;
        channelMetaData.byteCount = FIXED_BYTE_PER_CHANNEL;
        channelMetaData.redChannelInx = 0;
        channelMetaData.greenChannelInx = 1;
        channelMetaData.blueChannelInx = 2;
    }

    // DEBUG/TEST
    public void test() {
        int volumeDataZeroCount = 0;
        java.util.TreeMap<Byte,Integer> frequencies = new TreeMap<Byte,Integer>();
        for (VolumeDataChunk chunk: channelVolumeData.getVolumeChunks() ) {
            for ( Byte aByte: chunk.getData() ) {
                if ( aByte == (byte)0 ) {
                    volumeDataZeroCount ++;
                }
                else {
                    Integer count = frequencies.get( aByte );
                    if ( count == null ) {
                        frequencies.put( aByte, 1 );
                    }
                    else {
                        frequencies.put( aByte, ++count );
                    }
                }
            }
        }

        for ( Byte key: frequencies.keySet() ) {
            logger.info("Encountered " + frequencies.get( key ) + " occurrences of " + key );
        }

        logger.info(
                "Found zeros in " + volumeDataZeroCount + " / " + channelVolumeData.length() + ", or " +
                ((double)volumeDataZeroCount/(double) channelVolumeData.length() * 100.0) + "%."
        );
    }

    //----------------------------------------IMPLEMENT MaskChanDataAcceptorI
    /**
     * This is called with data to be loaded.
     *
     * @param maskNumber describes all points belonging to all pairs.
     * @param position where in the linear volume coords does this go?
     * @return total positions applied.
     * @throws Exception thrown by called methods or if bad inputs are received.
     */
    @Override
    public int addMaskData(Integer maskNumber, long position, long x, long y, long z) throws Exception {
        throw new IllegalArgumentException( "Not implemented" );
    }

    /**
     * Go to position indicated, and add the single raw byte taken from each
     * channel-data at next counter position.  Add each such byte to the output
     * position provided.
     * Width of channel data is allowed by this routine to vary for each call.  Width will be
     * computed from the size of the incoming byte array.
     *
     * @param orignalMaskNum this is the ultimate mask value that has gone into the mask volume.
     * @param volumePosition where it goes.  Not multiplied by number of channels.  Not an offset into the channel data!
     * @param channelMetaData helps interpret the placement of channel data, where applicable.
     * @return total positions applied.
     * @throws Exception
     */
    @NotThreadSafe( why="writes to channel interp. May be called with diff masks.  Not synchronized." )
    @Override
    public int addChannelData(
            Integer orignalMaskNum,
            byte[] channelData,
            long volumePosition,
            long x, long y, long z,
            ChannelMetaData channelMetaData
    ) throws Exception {
        init();

        long targetPos = volumePosition * this.channelMetaData.channelCount * FIXED_BYTE_PER_CHANNEL;
        channelInterpreter.interpretChannelBytes(channelMetaData, this.channelMetaData, orignalMaskNum, channelData, targetPos);

        return 1;
    }

    /**
     * This tells the caller: only call me with channel data.  This is a channel-data builder.
     * @return channel
     */
    @Override
    public Acceptable getAcceptableInputs() {
        return Acceptable.channel;
    }

    /**
     * Note: this can throw Null Pointer Exception, if channel meta data has not yet been set.
     * @return number of channels in file.
     */
    @Override
    public int getChannelCount() {
        checkReady();
        return channelMetaData.channelCount;
    }

    /**
     * Use this to store in the channel meta data.
     * ORDER DEPENDENCY: call this before any affected getters.
     *
     * @param metaData what's to know about channels.
     */
    @Override
    public synchronized void setChannelMetaData(ChannelMetaData metaData) {
        if ( channelMetaData == null ) {
            this.channelMetaData = metaData;
            needsChannelInit = true;
        }
        else {
            StringBuilder errSb = new StringBuilder();
            if ( metaData.rawChannelCount > this.channelMetaData.rawChannelCount ) {
                errSb.append(
                        String.format(
                                COUNT_DISCREPANCY_FORMAT,
                                "Channel",
                                this.channelMetaData.rawChannelCount,
                                metaData.rawChannelCount
                        )
                );
            }
            if ( metaData.byteCount > this.channelMetaData.byteCount ) {
                errSb.append(
                        String.format(
                                COUNT_DISCREPANCY_FORMAT,
                                "Byte",
                                this.channelMetaData.byteCount,
                                metaData.byteCount
                        )
                );
            }
            if ( errSb.length() > 0 ) {
                throw new RuntimeException( errSb.toString() );
            }
        }
    }

    @Override
    public void populateVolumeAcceptor(VolumeDataAcceptor dataAcceptor) {
        logger.debug( "Populating volume acceptor." );
        dataAcceptor.setTextureData( buildTextureData() );
    }

    //----------------------------------------IMPLEMENT TextureBuilderI
    @Override
    public TextureDataI buildTextureData() {
        if ( needsChannelInit ) {
            throw new RuntimeException( "Internal Error: attempting to produce final data, when no data has been added." );
        }

        if ( channelInterpreter != null )
            channelInterpreter.close();

        TextureDataI textureData;
        double downSampleRate = settings.getAcceptedDownsampleRate();
        if ( downSampleRate != 0.0   &&   downSampleRate != 1.0 ) {
            DownSampler downSampler = new DownSampler( paddedSx, paddedSy, paddedSz );
            DownSampler.DownsampledTextureData downSampling = downSampler.getDownSampledVolume(
                    channelVolumeData,
                    channelMetaData.channelCount* bytesPerChannel,
                    downSampleRate,
                    downSampleRate,
                    downSampleRate
            );
            textureData = new TextureDataBean(
                    downSampling.getVolume(), downSampling.getSx(), downSampling.getSy(), downSampling.getSz()
            );
            textureData.setVolumeMicrometers(
                    new Double[]{
                            (double) downSampling.getSx(), (double)downSampling.getSy(), (double)downSampling.getSz()
                    }
            );
        }
        else {
            textureData = new TextureDataBean(
                    channelVolumeData, (int)paddedSx, (int)paddedSy, (int)paddedSz
            );
            textureData.setVolumeMicrometers( new Double[] { (double)paddedSx, (double)paddedSy, (double)paddedSz } );
        }
        textureData.setChannelCount( channelMetaData.channelCount );

        textureData.setColorSpace( MultiTexVolumeBrick.TextureColorSpace.COLOR_SPACE_LINEAR );
        textureData.setVoxelMicrometers(new Double[]{1.0, 1.0, 1.0});
        textureData.setByteOrder(ByteOrder.nativeOrder());
        textureData.setPixelByteCount(bytesPerChannel);
        textureData.setFilename( "Channel Data" );
        textureData.setInverted( false );
        textureData.setCoordCoverage( coordCoverage );

        // NOTE: use of GL_LINEAR is usually preferred when sending intensity data.  However, with the modifications
        // that the shader is making at the "fragment" level (which seem to occur after this interpolation), ghost
        // effects appear around shader-modified voxels.
        textureData.setInterpolationMethod( GL2.GL_NEAREST );

        // This set of inputs works against uploaded MP4 files.  T.o.Writing: YCD
        /*
        textureData.setExplicitVoxelComponentType( GL2.GL_UNSIGNED_INT_8_8_8_8 );
        textureData.setExplicitVoxelComponentOrder( GL2.GL_RGBA );
        textureData.setExplicitInternalFormat( GL2.GL_RGBA16 );
         */

        if ( bytesPerChannel == 1 )
            textureData.setExplicitVoxelComponentType( GL2.GL_BYTE ); //GL2.GL_UNSIGNED_INT_8_8_8_8 );
        else if ( bytesPerChannel == 2 )
            textureData.setExplicitVoxelComponentType( GL2.GL_UNSIGNED_SHORT );
        textureData.setExplicitVoxelComponentOrder( GL2.GL_RGBA );
        textureData.setExplicitInternalFormat( GL2.GL_RGBA );

        textureData.setRenderables( renderableBeans );

        //  Because all have been tried with failure, assuming that the intuitive type of 8/8/8/8 is correct.
        // YCD textureData.setExplicitVoxelComponentType(GL2.GL_UNSIGNED_INT_8_8_8_8_REV);
        //  Invalid memory access of location 0x800000010 rip=0x112a70227 textureData.setExplicitVoxelComponentType(GL2.GL_INT);
        // YCD textureData.setExplicitVoxelComponentType(GL2.GL_BYTE);
        //  Invalid memory access of location 0x800000000 rip=0x1113fe53b textureData.setExplicitVoxelComponentType(GL2.GL_UNSIGNED_INT);

        // textureData.setExplicitInternalFormat( GL2.GL_ALPHA8 );  // Time of Writing: yields black screen.
        // YCD textureData.setExplicitInternalFormat( 4 );
        // YCD textureData.setExplicitInternalFormat( GL2.GL_RGBA );
        // YCD textureData.setExplicitInternalFormat( GL2.GL_SRGB8 );

        // YCD textureData.setExplicitInternalFormat( GL2.GL_SRGB8_ALPHA8 );
        // YCD textureData.setExplicitInternalFormat( GL2.GL_RGB8 );
        //        textureData.setExplicitInternalFormat( GL2. );
        //        textureData.setExplicitInternalFormat( GL2. );

        // TEMP.
        //dumpVolume();

        // NOTE: if this is later needed, need to have a decider for the notion of luminance.
        //        if (! isLuminance  &&  (textureData.getPixelByteCount() == 4) ) {
        //            setAlphaToSaturateColors( textureData.getColorSpace() );
        //        }

        return textureData;
    }

    //-------------------------END:-----------IMPLEMENT MaskChanDataAcceptorI

    //----------------------------------------HELPER METHODS
    /** Call this prior to any update-data operations. */
    private void init() {
        if ( needsChannelInit) {
            synchronized (this) {
                checkReady();
                logger.debug( "Initialize called..." );

                // The size of any one voxel will be the number of channels times the bytes per channel.
                if ( channelMetaData.rawChannelCount == 3 ) {
                    // Round out to four.
                    ChannelMetaData newChannelMetaData = cloneChannelMetaData();
                    newChannelMetaData.channelCount = channelMetaData.rawChannelCount + 1;
                    channelMetaData = newChannelMetaData;
                    logger.debug(
                            "Padding out the channel count from {} to {}.",
                            channelMetaData.rawChannelCount, channelMetaData.channelCount
                    );
                }
                else if ( channelMetaData.rawChannelCount == 2 ) {
                    // Round out to four.
                    ChannelMetaData newChannelMetaData = cloneChannelMetaData();
                    newChannelMetaData.channelCount = channelMetaData.rawChannelCount + 2;
                    channelMetaData = newChannelMetaData;
                    logger.debug(
                            "Padding out the channel count from {} to {}.",
                            channelMetaData.rawChannelCount, channelMetaData.channelCount
                    );
                }
                long arrayLength = paddedSx * paddedSy * paddedSz *
                        channelMetaData.byteCount * channelMetaData.channelCount;
                if ( arrayLength == 0 ) {
                    throw new IllegalArgumentException(
                            "Array length of zero, for all data."
                    );
                }

                if ( sx > Integer.MAX_VALUE || sy > Integer.MAX_VALUE || sz > Integer.MAX_VALUE ) {
                    throw new IllegalArgumentException(
                            "One or more of the axial lengths (" + sx + "," + sy + "," + sz +
                                    ") exceeds max value for an integer.  " +
                                    "If this is truly required, code redesign will be necessary."
                    );
                }

                if ( channelVolumeData == null ) {
                    channelVolumeData = new VeryLargeVolumeData(
                            (int)paddedSx, (int)paddedSy, (int)paddedSz, bytesPerChannel * channelMetaData.channelCount
                    );
                    // OLD WAY:
                    //  channelVolumeData = new VolumeDataBean( (int)arrayLength, (int)paddedSx, (int)paddedSy, (int)paddedSz );
                }

                logger.debug(
                        "Raw channel count: {}, full channel count: {}.",
                        channelMetaData.rawChannelCount,
                        channelMetaData.channelCount
                );
                if ( maskVolumeData == null ) {
                    channelInterpreter = new FirstInToByteInterpreter(channelVolumeData);
                }
                else {
                    channelInterpreter = new ChannelInterpreterToByte(channelVolumeData, maskVolumeData, multiMaskTracker);
                }

                needsChannelInit = false;
            }
        }
    }

    private ChannelMetaData cloneChannelMetaData() {
        ChannelMetaData newChannelMetaData = new ChannelMetaData();
        newChannelMetaData.rawChannelCount = channelMetaData.rawChannelCount;
        newChannelMetaData.byteCount = channelMetaData.byteCount;
        newChannelMetaData.blueChannelInx = channelMetaData.blueChannelInx;
        newChannelMetaData.greenChannelInx = channelMetaData.greenChannelInx;
        newChannelMetaData.redChannelInx = channelMetaData.redChannelInx;
        return newChannelMetaData;
    }

    private void checkReady() {
        if ( channelMetaData == null ) {
            throw new IllegalStateException( "Must set channel meta data prior to this call." );
        }
        if ( sx == 0L ) {
            throw new IllegalStateException( "Must have volume size parameters set prior to this call." );
        }
    }

}
