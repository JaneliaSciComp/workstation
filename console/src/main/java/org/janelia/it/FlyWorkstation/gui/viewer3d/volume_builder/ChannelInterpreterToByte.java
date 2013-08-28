package org.janelia.it.FlyWorkstation.gui.viewer3d.volume_builder;

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
    private final byte[] volumeData;

    private int maxValue = 0;
    private final Logger logger = LoggerFactory.getLogger( ChannelInterpreterToByte.class );

    public ChannelInterpreterToByte(byte[] volumeData) {
        this.volumeData = volumeData;

    }

    @Override
    public void interpretChannelBytes(ChannelMetaData srcChannelMetaData, ChannelMetaData targetChannelMetaData, byte[] channelData, int targetPos) {
        if ( srcChannelMetaData.byteCount == 1  &&  srcChannelMetaData.channelCount == 1 ) {
            // 1:1 straight copy to volume.
            System.arraycopy(channelData, 0, volumeData, targetPos, srcChannelMetaData.rawChannelCount);
        }
        else {
            int[] orderedRgbIndexes = srcChannelMetaData.getOrderedRgbIndexes();

            // N:1 divide by max-byte.
            for ( int i = 0; i < srcChannelMetaData.rawChannelCount; i++ ) {
                int finalValue = 0;
                // This is a local down-sample from N-bytes-per-channel to the required number only.
                for ( int j = 0; j < srcChannelMetaData.byteCount; j++ ) {
                    int nextByte = channelData[ (i * srcChannelMetaData.byteCount) + j ];
                    if ( nextByte < 0 )
                        nextByte += 256;
                    int shifter = srcChannelMetaData.byteCount - j - 1;
                    finalValue += (nextByte << (8 * shifter));
                }
                if ( srcChannelMetaData.byteCount == 2 )
                    finalValue /= 256;
                else if ( srcChannelMetaData.byteCount == 3 )
                    finalValue /= 65535;

                if ( finalValue > maxValue ) {
                    maxValue = finalValue;
                }

                //  block of in-memory, interleaving the channels as the offsets follow.
                int channelInx = orderedRgbIndexes[ i ];

                //synchronized (this) {
                if ( targetPos + channelInx > 0  &&  (volumeData.length > targetPos+channelInx)) {
                    // Here enforced: keep only certain bits of any previous channel.  Allow low bits to vary.
                    volumeData[ targetPos + channelInx ] &= (byte)0xF0;
                    volumeData[ targetPos + channelInx ] |= (byte)finalValue;
                }
                else {
                    logger.debug("Outside the box");
                }
                //}
            }
        }

        // Pad out to the end, to create the alpha byte.
        if ( targetChannelMetaData.channelCount >= ( srcChannelMetaData.channelCount + 1 ) ) {
            if ( targetPos + targetChannelMetaData.channelCount - 1 > 0  &&  (volumeData.length > targetPos + targetChannelMetaData.channelCount - 1))
                volumeData[ targetPos + targetChannelMetaData.channelCount - 1 ] = (byte)255;
            else
                logger.error("Outside the box");
        }
    }

    @Override
    public void close() {
        logger.info( "Maximum value found during channel interpretation was {}.", maxValue );
    }
}
