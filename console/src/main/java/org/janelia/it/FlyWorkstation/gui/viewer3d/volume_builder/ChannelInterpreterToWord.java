package org.janelia.it.FlyWorkstation.gui.viewer3d.volume_builder;

import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.ChannelMetaData;

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
public class ChannelInterpreterToWord implements ChannelInterpreterI {
    private byte[] volumeData;

    public ChannelInterpreterToWord( byte[] volumeData) {
        this.volumeData = volumeData;

    }

    @Override
    public void interpretChannelBytes(ChannelMetaData channelMetaData, ChannelMetaData targetChannelMetaData, byte[] channelData, int targetPos) {
        int[] orderedRgbIndexes = channelMetaData.getOrderedRgbIndexes();

        for ( int i = 0; i < channelMetaData.rawChannelCount; i++ ) {
            for ( int j = 0; j < channelMetaData.byteCount; j++ ) {
                //  block of in-memory, interleaving the channels as the offsets follow.
                int channelInx = orderedRgbIndexes[ i ];

                int channelDataOffset = channelInx * channelMetaData.byteCount + j;
                volumeData[(targetPos + channelDataOffset)] =
                        channelData[ channelDataOffset ];
            }

        }

        // Pad out to the end, to create the alpha byte(s).
        if ( channelMetaData.channelCount > channelMetaData.rawChannelCount ) {
            for ( int j = channelMetaData.rawChannelCount; j < channelMetaData.channelCount; j++ ) {
                for ( int byteOffs = 0; byteOffs < channelMetaData.byteCount; byteOffs ++ ) {
                    volumeData[ targetPos + (j * channelMetaData.byteCount) + byteOffs ] = (byte)255;
                }
            }
        }
    }

    @Override
    public void close() {}
}
