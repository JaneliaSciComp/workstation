package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.channel_split;

import org.janelia.it.jacs.compute.access.loader.ChannelMetaData;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/10/13
 * Time: 10:21 AM
 *
 * Utility methods used in channel splitting.
 */
public class ChannelSplitUtils {
    public static int getMaxValue(ChannelMetaData channelMetaData, byte[] channelsData) {
        int maxValue = Integer.MIN_VALUE;

        // Must walk through all channels, finding max, converting each to its full-sized original value,
        // for comparison.
        for ( int channelInx = 0; channelInx < channelMetaData.rawChannelCount; channelInx ++ ) {
            int nextValue = 0;
            for ( int byteInx = 0; byteInx < channelMetaData.byteCount; byteInx++ ) {
                // The volume mask is the one currently in use.  This could be a single or multi-mask.
                int volumeLoc = byteInx + (channelInx * channelMetaData.byteCount);
                int channelByte = channelsData[volumeLoc];
                if ( channelByte < 0 ) {
                    channelByte += 256;
                }
                nextValue |= channelByte << (8*byteInx);
            }
            if ( nextValue > maxValue ) {
                maxValue = nextValue;
            }
        }
        return maxValue;
    }

}
