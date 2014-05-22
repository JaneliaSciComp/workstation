package org.janelia.it.workstation.gui.alignment_board_viewer.volume_builder;

import org.janelia.it.workstation.gui.alignment_board.channel_split.ChannelSplitStrategyI;
import org.janelia.it.workstation.gui.viewer3d.masking.VolumeDataI;
import org.janelia.it.workstation.gui.alignment_board.loader.ChannelMetaData;
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
    private ByteChannelDelegate byteChannelDelegate;

    private final Logger logger = LoggerFactory.getLogger( ChannelInterpreterToByte.class );
    private org.janelia.it.workstation.gui.alignment_board_viewer.channel_split.ChannelSplitStrategyFactory splitStrategyFactory;

    /**
     * Construct with volume data to be modified, as well as the mask volume for reference.
     *
     * @param signalVolume to modify with latest contribution of channel bytes.
     * @param wholeMaskVolume to reference for mask in use.
     * @param multiMaskTracker to help with mask-based channel data changes.
     */
    public ChannelInterpreterToByte(VolumeDataI signalVolume, VolumeDataI wholeMaskVolume, org.janelia.it.workstation.gui.alignment_board_viewer.masking.MultiMaskTracker multiMaskTracker) {
        this.wholeSignalVolume = signalVolume;
        this.wholeMaskVolume = wholeMaskVolume;
        this.byteChannelDelegate = new ByteChannelDelegate( signalVolume );
        if ( wholeMaskVolume == null || wholeSignalVolume == null ) {
            throw new IllegalArgumentException("Null not allowed here.");
        }
        splitStrategyFactory = new org.janelia.it.workstation.gui.alignment_board_viewer.channel_split.ChannelSplitStrategyFactory( multiMaskTracker );
    }

    /**
     * Takes mask-replacement into account, interpreting the data fed here; also ensures output channels fit into one
     * byte each.
     */
    @Override
    public void interpretChannelBytes(
            ChannelMetaData srcChannelMetaData,
            ChannelMetaData targetChannelMetaData,
            int orignalMaskNum,
            byte[] channelData,
            long targetPos
    ) {

        int multiMaskId = getMaskValue(targetPos, targetChannelMetaData.channelCount, org.janelia.it.workstation.gui.alignment_board_viewer.volume_builder.RenderablesMaskBuilder.UNIVERSAL_MASK_BYTE_COUNT);  //TODO consider passing mask-size into the interpreter or moving the constant somewhere more general.

        if ( srcChannelMetaData.byteCount == 1  &&  srcChannelMetaData.channelCount == 1  &&  multiMaskId == orignalMaskNum ) {
            // 1:1 straight copy to volume.
            wholeSignalVolume.setValueAt(targetPos, channelData[0]);
        }
        else {
            // First get the size-adjusted channel bytes.  These are suited to the target channel characteristics,
            // rather than the source characteristics--those from the input file. Put differently: adapt in to out.
            byte[] targetChannelBytes = byteChannelDelegate.adjustChannelWidth(
                    srcChannelMetaData, targetChannelMetaData, channelData, targetPos
            );

            // At this point, multiplex the just-created target bytes so this alternate mask's data are positioned
            // correctly within the final value pushed to the volume.
            ChannelSplitStrategyI channelSplitStrategy = splitStrategyFactory.getStrategyForMask( multiMaskId );
            if ( channelSplitStrategy != null ) {
                targetChannelBytes = channelSplitStrategy.getUpdatedValue(
                        targetChannelMetaData, orignalMaskNum, targetChannelBytes, multiMaskId
                );
            }
            // NOTE: on DEBUG.  This code can serve to verify that the bytes from the incoming sub-masks (the
            //  pre-ordered renderables under a given multimask) are donating their input bytes to the volume
            //  in the proper order, and into the proper byte "slots".  If used in future, first the three
            //  numeric values must be adjusted.  Find two sub-masks that go into a multimask.  Those will replace
            //  the '4' and '33' below.  Find the multimask they make up.  That replaces '64'.

            //  DEBUG if ( (orignalMaskNum  == 4 || orignalMaskNum==33) && multiMaskId == 64 ) {
            //  DEBUG System.err.println("Position: " + targetPos + ", "+orignalMaskNum);
            //  DEBUG }
            for ( int i = 0; i < targetChannelMetaData.channelCount; i++ ) {
                //  block of in-memory, interleaving the channels as the offsets follow.
                if ( targetPos + i >= 0  &&  (wholeSignalVolume.length() > targetPos+i)) {
                    // Here enforced: multiplexing the channel data by inserting latest.
                    int existingValue = wholeSignalVolume.getValueAt(targetPos + i);
                    if ( existingValue < 0 ) existingValue += 256;
                    //  DEBUG if ( (orignalMaskNum  == 4 || orignalMaskNum==33) && multiMaskId == 64 ) {
                    //  DEBUG System.err.print(" ("+i+"):"+existingValue+"|"+targetChannelBytes[i]);
                    //  DEBUG }
                    wholeSignalVolume.setValueAt(
                            targetPos + i,
                            (byte) (existingValue | targetChannelBytes[i])
                    );
                }
                else {
                    logger.error("Outside the box at volume writeback time.");
                }
            }
            //  DEBUG if ( (orignalMaskNum  == 4 || orignalMaskNum==33) && multiMaskId == 64 ) {
            //  DEBUG System.err.println();
            //  DEBUG }
        }

        if ( orignalMaskNum == multiMaskId ) {
            byteChannelDelegate.padChannelBytes( srcChannelMetaData, targetChannelMetaData, targetPos );
        }


    }

    @Override
    public void close() {
        logger.info( "Maximum value found during channel interpretation was {}.", byteChannelDelegate.getMaxValue() );
    }

    /** Goes to the mask volume, finds and reconstructs the mask at position given. */
    private int getMaskValue( long position, int targetChannelWidth, int maskByteCount ) {
        // Find the appropriate slot in the mask data, and get its value.
        int volumeMask = 0;
        if ( wholeMaskVolume.isVolumeAvailable() ) {
            long volumeLoc = (position / targetChannelWidth) * maskByteCount;
            // Assumed little-endian.
            for ( int j = 0; j < maskByteCount; j++ ) {
                // The volume mask is the one currently in use.  This could be a single or multi-mask.
                int maskByte = wholeMaskVolume.getValueAt(volumeLoc);
                if ( maskByte < 0 )
                    maskByte += 256;
                volumeMask += maskByte << (8*j);
                volumeLoc++;
            }
        }
        else {
            throw new RuntimeException("No volume data available.  Cannot get mask data.");
        }

        return volumeMask;

    }
}
