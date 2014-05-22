package org.janelia.it.workstation.gui.alignment_board_viewer.volume_builder;

import org.janelia.it.workstation.gui.alignment_board.loader.ChannelMetaData;
import org.janelia.it.workstation.gui.viewer3d.masking.VolumeDataI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/26/13
 * Time: 10:10 AM
 *
 * Shared code from multiple byte-wise channel writers.
 */
public class ByteChannelDelegate {

    private int maxValue;
    private VolumeDataI wholeSignalVolume;
    private Logger logger = LoggerFactory.getLogger( ByteChannelDelegate.class );

    public ByteChannelDelegate( VolumeDataI wholeSignalVolume ) {
        this.wholeSignalVolume = wholeSignalVolume;
    }

    public byte[] adjustChannelWidth(
            ChannelMetaData srcChannelMetaData,
            ChannelMetaData targetChannelMetaData,
            byte[] channelData,
            long targetPos
    ) {
        int[] orderedRgbIndexes = srcChannelMetaData.getOrderedRgbIndexes();
        byte[] targetChannelBytes = new byte[ targetChannelMetaData.byteCount * targetChannelMetaData.channelCount ];
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
            if ( targetPos + channelInx > 0  &&  (wholeSignalVolume.length() > targetPos+channelInx)) {
                targetChannelBytes[ channelInx ] = (byte)finalValue;
            }
            else {
                logger.debug("Outside the box");
            }
            //}
        }
        return targetChannelBytes;
    }

    public void padChannelBytes(
            ChannelMetaData srcChannelMetaData,
            ChannelMetaData targetChannelMetaData,
            long targetPos
    ) {
        // Pad out to the end, to create the alpha byte.
        if ( targetChannelMetaData.channelCount >= ( srcChannelMetaData.channelCount + 1 ) ) {
            if ( targetPos + targetChannelMetaData.channelCount - 1 > 0  &&  (wholeSignalVolume.length() > targetPos + targetChannelMetaData.channelCount - 1))
                wholeSignalVolume.setValueAt(targetPos + targetChannelMetaData.channelCount - 1, (byte) 255);
            else
                logger.error("Outside the box");
        }
    }

    public int getMaxValue() {
        return maxValue;
    }

}
