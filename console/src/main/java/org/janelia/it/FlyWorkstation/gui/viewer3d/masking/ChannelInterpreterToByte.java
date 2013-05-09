package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.ChannelMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/10/13
 * Time: 2:09 PM
 *
 * This decides how to deal with incoming channel data, for a volume builder.  It is set aside as its own object
 * so that the complex decisions of downsampling within a channle, can be isolated, and setting up fixed (over whole
 * loop) values can be done in only one place.
 */
public class ChannelInterpreterToByte implements ChannelInterpreterI {
    private byte[] volumeData;

    private int maxValue = 0;
    private Logger logger = LoggerFactory.getLogger( ChannelInterpreterToByte.class );

    public ChannelInterpreterToByte(byte[] volumeData) {
        this.volumeData = volumeData;

    }

    @Override
    public void interpretChannelBytes(ChannelMetaData channelMetaData, byte[] channelData, int targetPos) {
        if ( channelMetaData.byteCount == 1  &&  channelMetaData.channelCount == 1 ) {
            // 1:1 straight copy to volume.
            for ( int channelInx = 0; channelInx < channelMetaData.rawChannelCount; channelInx++ ) {
                volumeData[ targetPos + channelInx ] = channelData[ channelInx ];
            }
        }
        else {
            int[] orderedRgbIndexes = channelMetaData.getOrderedRgbIndexes();

            // N:1 divide by max-byte.
            for ( int i = 0; i < channelMetaData.rawChannelCount; i++ ) {
                int finalValue = 0;
                // This is a local down-sample from N-bytes-per-channel to the required number only.
                for ( int j = 0; j < channelMetaData.byteCount; j++ ) {
                    int nextByte = channelData[ (i * channelMetaData.byteCount) + j ];
                    if ( nextByte < 0 )
                        nextByte += 256;
                    int shifter = channelMetaData.byteCount - j - 1;
                    finalValue += (nextByte << (8 * shifter));
                }
                if ( channelMetaData.byteCount == 2 )
                    finalValue /= 256;
                else if ( channelMetaData.byteCount == 3 )
                    finalValue /= 65535;

                if ( finalValue > maxValue ) {
                    maxValue = finalValue;
                }

                //  block of in-memory, interleaving the channels as the offsets follow.
                int channelInx = orderedRgbIndexes[ i ];
                volumeData[ targetPos + channelInx ] = (byte)finalValue;
            }
        }

        // Pad out to the end, to create the alpha byte.
        if ( channelMetaData.channelCount > channelMetaData.rawChannelCount ) {
            volumeData[ targetPos + channelMetaData.channelCount - 1 ] = (byte)255;
        }
    }

    @Override
    public void close() {
        logger.info( "Maximum value found during channel interpretation was {}.", maxValue );
    }
}
