package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder;

import org.janelia.it.jacs.shared.loader.ChannelMetaData;
import org.janelia.it.jacs.shared.loader.volume.VolumeDataI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/26/13
 * Time: 10:02 AM
 *
 * This impl will accept only the first value setting fed into a given position.  Subsequent settings will be ignored.
 */
public class FirstInToByteInterpreter implements ChannelInterpreterI {
    private VolumeDataI wholeSignalVolume;
    private ByteChannelDelegate byteChannelDelegate;
    private Logger logger = LoggerFactory.getLogger( FirstInToByteInterpreter.class );

    /** Cache the volume data for the result.  Write to this, and read from it. */
    public FirstInToByteInterpreter( VolumeDataI channelVolumeData ) {
        this.wholeSignalVolume = channelVolumeData;
        byteChannelDelegate = new ByteChannelDelegate( channelVolumeData );
    }

    /**
     * Ensures output channels fit into one byte each; enforces a first-in-kept policy, if multiple renderables
     * visit the same channel (aka intensity, aka signal) position.
     */
    @Override
    public void interpretChannelBytes(
            ChannelMetaData srcChannelMetaData,
            ChannelMetaData targetChannelMetaData,
            int fileMaskNum,
            byte[] channelData,
            long targetPos
    ) {
        boolean wroteBack = false;

        if ( srcChannelMetaData.byteCount == 1  &&  srcChannelMetaData.channelCount == 1 ) {
            // 1:1 straight copy to volume, iff there was nothing at that pos before.
            if ( wholeSignalVolume.getValueAt( targetPos ) == 0 ) {
                wholeSignalVolume.setValueAt(targetPos, channelData[0]);
            }
        }
        else {
            // First get the size-adjusted channel bytes.  These are suited to the target channel characteristics,
            // rather than the source characteristics--those from the input file. Put differently: adapt in to out.
            byte[] targetChannelBytes = byteChannelDelegate.adjustChannelWidth(
                    srcChannelMetaData, targetChannelMetaData, channelData, targetPos
            );

            // Need to figure out if anything had been set in these channel bytes before.  Non-zero in any channel
            // means no-go.
            int maxChannelByte = 0;
            for ( int i = 0; i < targetChannelMetaData.channelCount; i++ ) {
                if ( targetPos + i >= 0  &&  (wholeSignalVolume.length() > targetPos+i)) {
                    int nextChannelByte = wholeSignalVolume.getValueAt( targetPos + i );
                    if ( nextChannelByte > maxChannelByte ) {
                        maxChannelByte = nextChannelByte;
                    }
                }
                else {
                    logger.error("Outside the box at volume writeback time.");
                }
            }

            // Here enforced: do not multiplex channel data.  Keep only the first setting.
            if ( maxChannelByte == 0 ) {
                wroteBack = true;
                for ( int i = 0; i < targetChannelMetaData.channelCount; i++ ) {
                    //  block of in-memory, interleaving the channels as the offsets follow.
                    wholeSignalVolume.setValueAt( targetPos + i, targetChannelBytes[i] );
                }
            }
        }

        if ( wroteBack ) {
            // Pad out to the end, to create the alpha byte.
            byteChannelDelegate.padChannelBytes( srcChannelMetaData, targetChannelMetaData, targetPos );
        }

    }

    @Override
    public void close() {
        logger.info( "Maximum value found during channel interpretation was {}.", byteChannelDelegate.getMaxValue() );
    }
}
