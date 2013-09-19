package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.channel_split.ChannelSplitStrategyFactory;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.MultiMaskTracker;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.VolumeDataI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.channel_split.ChannelSplitStrategyI;
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
    private VolumeDataI wholeSignalVolume;
    private VolumeDataI wholeMaskVolume;

    private int maxValue = 0;
    private final Logger logger = LoggerFactory.getLogger( ChannelInterpreterToByte.class );
    private ChannelSplitStrategyFactory splitStrategyFactory;

    /**
     * Construct with volume data to be modified, as well as the mask volume for reference.
     *
     * @param signalVolume to modify with latest contribution of channel bytes.
     * @param wholeMaskVolume to reference for mask in use.
     * @param multiMaskTracker to help with mask-based channel data changes.
     */
    public ChannelInterpreterToByte(VolumeDataI signalVolume, VolumeDataI wholeMaskVolume, MultiMaskTracker multiMaskTracker) {
        this.wholeSignalVolume = signalVolume;
        this.wholeMaskVolume = wholeMaskVolume;
        splitStrategyFactory = new ChannelSplitStrategyFactory( multiMaskTracker );
    }

    @Override
    public void interpretChannelBytes(ChannelMetaData srcChannelMetaData, ChannelMetaData targetChannelMetaData, int orignalMaskNum, byte[] channelData, int targetPos) {

        int multiMaskId = getMaskValue( targetPos, targetChannelMetaData.channelCount, RenderablesMaskBuilder.UNIVERSAL_MASK_BYTE_COUNT );  //TODO consider passing mask-size into the interpreter or moving the constant somewhere more general.

        if ( srcChannelMetaData.byteCount == 1  &&  srcChannelMetaData.channelCount == 1  &&  multiMaskId == orignalMaskNum ) {
            // 1:1 straight copy to volume.
            wholeSignalVolume.setCurrentValue( targetPos, channelData[ 0 ] );
        }
        else {
            // First get the size-adjusted channel bytes.  These are suited to the target channel characteristics,
            // rather than the source characteristics--those from the input file. Put differently: adapt in to out.
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

            // At this point, multiplex the just-created target bytes so any alternate masks are represented.
            ChannelSplitStrategyI channelSplitStrategy = splitStrategyFactory.getStrategyForMask( multiMaskId );
            if ( channelSplitStrategy != null ) {
                targetChannelBytes = channelSplitStrategy.getUpdatedValue(
                        targetChannelMetaData, orignalMaskNum, targetChannelBytes, multiMaskId
                );
            }
            for ( int i = 0; i < targetChannelMetaData.channelCount; i++ ) {
                //  block of in-memory, interleaving the channels as the offsets follow.
                if ( targetPos + i >= 0  &&  (wholeSignalVolume.length() > targetPos+i)) {
                    // Here enforced: multiplexing the channel data by "OR"-ing in the latest.
                    wholeSignalVolume.setCurrentValue(
                            targetPos + i,
                            (byte)(wholeSignalVolume.getCurrentValue( targetPos + i ) | targetChannelBytes[ i ])
                    );
                }
                else {
                    logger.error("Outside the box at volume writeback time.");
                }
            }
        }

        // Pad out to the end, to create the alpha byte.
        if ( targetChannelMetaData.channelCount >= ( srcChannelMetaData.channelCount + 1 ) &&
             multiMaskId == orignalMaskNum ) {
            if ( targetPos + targetChannelMetaData.channelCount - 1 > 0  &&  (wholeSignalVolume.length() > targetPos + targetChannelMetaData.channelCount - 1))
                wholeSignalVolume.setCurrentValue( targetPos + targetChannelMetaData.channelCount - 1, (byte)255 );
            else
                logger.error("Outside the box");
        }
    }

    @Override
    public void close() {
        logger.info( "Maximum value found during channel interpretation was {}.", maxValue );
    }

    /** Goes to the mask volume, finds and reconstructs the mask at position given. */
    private int getMaskValue( int position, int targetChannelWidth, int maskByteCount ) {
        // Find the appropriate slot in the mask data, and get its value.
        int volumeMask = 0;
        if ( wholeMaskVolume.isVolumeAvailable() ) {
            int volumeLoc = (position / targetChannelWidth) * maskByteCount;
            // Assumed little-endian.
            for ( int j = 0; j < maskByteCount; j++ ) {
                // The volume mask is the one currently in use.  This could be a single or multi-mask.
                int maskByte = wholeMaskVolume.getCurrentValue( volumeLoc );
                if ( maskByte < 0 )
                    maskByte += 256;
                volumeMask += maskByte << (8*j);
                volumeLoc++;
            }
        }
        else {
            throw new RuntimeException("No volume data available.  Cannot add mask data.");
        }

        return volumeMask;

    }
}
