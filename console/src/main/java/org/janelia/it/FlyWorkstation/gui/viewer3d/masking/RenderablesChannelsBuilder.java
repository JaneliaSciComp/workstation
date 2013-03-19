package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.ChannelMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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
public class RenderablesChannelsBuilder extends RenderablesVolumeBuilder {

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

    //----------------------------------------CONFIGURATOR METHODS/C'TORs
    public void setByteCount( int byteCount ) {
        this.channelMetaData.byteCount = byteCount;
    }

    public void setChannelCount( int channelCount ) {
        this.channelMetaData.channelCount = channelCount;
    }

    //----------------------------------------ABSTRACT OVERRIDE IMPLEMENTATIONS
    /** Call this prior to any update-data operations. */
    @Override
    public void init() {
        if ( needsChannelInit) {
            logger.info( "Initialize called..." );
            long arrayLength = sx * sy * sz * channelMetaData.byteCount;
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

            volumeData = new byte[ (int) arrayLength ];
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
     * Go to position indicated, and add this array of raw bytes.
     *
     * @param position where it goes. Width of channel data is expected to be same for each call.
     * @return total positions applied.
     * @throws Exception
     */
    @Override
    public int addChannelData(List<byte[]> channelData, long position) throws Exception {
        init();
        // Assumed little-endian and two bytes.
        byte[] data = volumeData;

        // TODO use the suggested R/G/B to specify the ordering, rather than simply using the 'i' offset.
        for ( int i = 0; i < getChannelByteCount(); i++ ) {
            byte[] nextChannel = channelData.get( i );
            //  block of in-memory, interleaving the channels as the offsets follow.
            data[ (int)position + i ] = nextChannel[ (int)position ];
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
    public int getChannelByteCount() {
        return channelMetaData.byteCount;
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
        this.setByteCount( metaData.byteCount );
        this.setChannelCount( metaData.channelCount );
    }

    //-------------------------END:-----------IMPLEMENT MaskChanDataAcceptorI

    //----------------------------------------HELPER METHODS
}
