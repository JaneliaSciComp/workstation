package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeBrick;
import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.ChannelMetaData;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.VolumeLoaderI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL2;
import java.nio.ByteOrder;
import java.util.Arrays;
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
public class RenderablesChannelsBuilder extends RenderablesVolumeBuilder implements VolumeLoaderI {

    private ChannelMetaData channelMetaData;
    private byte[] volumeData;

    protected boolean needsChannelInit = false; // Initialized for emphasis.
    private Logger logger = LoggerFactory.getLogger( RenderablesChannelsBuilder.class );

    public RenderablesChannelsBuilder() {
        super();  // ...and I _mean_ that!
        needsChannelInit = true; // Must initialize the channel-specific data.
    }

    // DEBUG/TEST
    public void test() throws Exception {
        int volumeDataZeroCount = 0;
        java.util.TreeMap<Byte,Integer> frequencies = new TreeMap<Byte,Integer>();
        for ( Byte aByte: volumeData ) {
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

        for ( Byte key: frequencies.keySet() ) {
            System.out.println("Encountered " + frequencies.get( key ) + " occurrences of " + key );
        }

        System.out.println("Found zeros in " + volumeDataZeroCount + " / " + volumeData.length + ", or " + ((double)volumeDataZeroCount/(double)volumeData.length * 100.0) + "%." );
    }

    //----------------------------------------ABSTRACT OVERRIDE IMPLEMENTATIONS
    /** Call this prior to any update-data operations. */
    @Override
    public void init() {
        if ( needsChannelInit) {
            logger.info( "Initialize called..." );
            // The size of any one voxel will be the number of channels times the bytes per channel.
            if ( channelMetaData.channelCount == 3 ) {
                // Round out to four.
                ChannelMetaData newChannelMetaData = new ChannelMetaData();
                newChannelMetaData.channelCount = channelMetaData.channelCount + 1;
                newChannelMetaData.byteCount = channelMetaData.byteCount;
                newChannelMetaData.blueChannelInx = channelMetaData.blueChannelInx;
                newChannelMetaData.greenChannelInx = channelMetaData.greenChannelInx;
                newChannelMetaData.redChannelInx = channelMetaData.redChannelInx;

                channelMetaData = newChannelMetaData;
            }
            long arrayLength = sx * sy * sz * channelMetaData.byteCount * channelMetaData.channelCount;
            if ( arrayLength > Integer.MAX_VALUE ) {
                throw new IllegalArgumentException(
                        "Total length of input: " + arrayLength  +
                                " exceeds maximum array size capacity.  " +
                                "If this is truly required, code redesign will be necessary."
                );
            }
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

            if ( volumeData == null ) {
                volumeData = new byte[ (int) arrayLength ];
            }
            needsChannelInit = false;
        }
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
    public int addMaskData(Integer maskNumber, long position) throws Exception {
        throw new IllegalArgumentException( "Not implemented" );
    }

    /**
     * Go to position indicated, and add the single raw byte taken from each
     * channel-data at next counter position.  Add each such byte to the output
     * position provided.
     * Width of channel data is expected to be same for each call.
     *
     * @param volumePosition where it goes.  Not multiplied by number of channels.  Not an offset into the channel data!
     * @return total positions applied.
     * @throws Exception
     */
    @Override
    public int addChannelData(byte[] channelData, long volumePosition) throws Exception {
        init();

        // Assumed little-endian and two bytes.
        int targetPos = (int)( volumePosition * channelMetaData.channelCount * channelMetaData.byteCount );

        // TODO use the suggested R/G/B to specify the ordering, rather than simply using the 'i' offset.
        for ( int i = 0; i < channelData.length; i++ ) {
            for ( int j = 0; j < channelMetaData.byteCount; j++ ) {
                //  block of in-memory, interleaving the channels as the offsets follow.
                volumeData[ targetPos + i + j ] = channelData[ i ];
            }

            // Pad out to the end.
            if ( channelMetaData.channelCount > channelData.length ) {
                for ( int j = channelData.length; j < channelMetaData.channelCount; j++ ) {
                    volumeData[ targetPos + j ] = (byte)255;
                }
            }
        }

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

    @Override
    public int getChannelCount() {
        return channelMetaData.channelCount;
    }

    /**
     * Use this to store in the channel meta data.
     * ORDER DEPENDENCY: call this before any affected getters.
     *
     * @param metaData what's to know about channels.
     */
    @Override
    public void setChannelMetaData(ChannelMetaData metaData) {
        this.channelMetaData = metaData;
        needsChannelInit = true;
    }

    @Override
    public void populateVolumeAcceptor(VolumeDataAcceptor dataAcceptor) {
        dataAcceptor.setTextureData( buildTextureData() );
    }

    //-------------------------END:-----------IMPLEMENT MaskChanDataAcceptorI

    //----------------------------------------HELPER METHODS
    private TextureDataI buildTextureData() {
        // TODO: the decisioning -- how much to downsample, based on info re graphics card.
        //
        DownSampler downSampler = new DownSampler( sx, sy, sz );
        DownSampler.DownsampledTextureData downSampling = downSampler.getDownSampledVolume(
                volumeData, 4, 2.0, 2.0, 2.0
        );
        TextureDataI textureData = new TextureDataBean(
                downSampling.getVolume(), downSampling.getSx(), downSampling.getSy(), downSampling.getSz()
        );
        textureData.setVolumeMicrometers(
                new Double[]{(double) downSampling.getSx(), (double)downSampling.getSy(), (double)downSampling.getSz() }
        );
        textureData.setChannelCount( channelMetaData.channelCount );

        textureData.setColorSpace( VolumeBrick.TextureColorSpace.COLOR_SPACE_LINEAR );
        textureData.setVoxelMicrometers(new Double[]{1.0, 1.0, 1.0});
        textureData.setByteOrder( ByteOrder.nativeOrder() );
        textureData.setPixelByteCount( channelMetaData.byteCount );
        textureData.setFilename( "Channel Data" );
        textureData.setInverted( false );

        textureData.setInterpolationMethod( GL2.GL_LINEAR );

        // This set of inputs works against uploaded MP4 files.  T.o.Writing: YCD
        /*
        textureData.setExplicitVoxelComponentType( GL2.GL_UNSIGNED_INT_8_8_8_8 );
        textureData.setExplicitVoxelComponentOrder( GL2.GL_RGBA );
        textureData.setExplicitInternalFormat( GL2.GL_RGBA16 );
         */

        textureData.setExplicitVoxelComponentType( GL2.GL_BYTE ); //GL2.GL_UNSIGNED_INT_8_8_8_8 );
        textureData.setExplicitVoxelComponentOrder( GL2.GL_RGBA );
        textureData.setExplicitInternalFormat( GL2.GL_RGBA );

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

    private void dumpVolume() {
        try {
            java.io.PrintStream bos = new java.io.PrintStream( new java.io.FileOutputStream( "/users/fosterl/file_dump.txt" ) );
            bos.println("VOLUME DATA BEGINS------");
            int nextPos = 0;
            byte[] inputArr = null;
            while ( nextPos < volumeData.length ) {
                inputArr = Arrays.copyOfRange( volumeData, nextPos, nextPos + (int)sx );
                for ( int i = 0; i < inputArr.length; i++ ) {
                    if ( inputArr[ i ] != 0 ) {
                        // Print this one.
                        bos.print( String.format( "%010d", nextPos ) );
                        bos.print( ":  ");
                        for ( int j = 0; j < inputArr.length; j++ ) {
                            if ( inputArr[ j ] == (byte) 0 ) {
                                bos.print( "  " );
                            }
                            else {
                                bos.print( String.format( "%02x", (int)inputArr[ j ] ) );
                            }
                        }
                        bos.println();

                        break;
                    }
                }
                nextPos += (int)sx;
            }

            bos.close();
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }
    }

}
