package org.janelia.it.workstation.gui.alignment_board_viewer.channel_split;

import org.janelia.it.workstation.gui.alignment_board.channel_split.ChannelSplitStrategyI;
import org.janelia.it.workstation.gui.alignment_board.loader.ChannelMetaData;
import org.janelia.it.workstation.gui.alignment_board_viewer.masking.MultiMaskTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/9/13
 * Time: 3:18 PM
 *
 * Implements a channel split, by giving a byte to each channel-data contender.
 */
public class ByteChannelSplitStrategy implements ChannelSplitStrategyI {
    private MultiMaskTracker multiMaskTracker;
    private Logger logger = LoggerFactory.getLogger( ByteChannelSplitStrategy.class );

    public ByteChannelSplitStrategy( MultiMaskTracker multiMaskTracker ) {
        this.multiMaskTracker = multiMaskTracker;
    }

    /**
     * Adjust the bytes of the channel data, to include a new sub-mask _of_ the multimask whose id is given.
     *
     * @param channelMetaData for num channels, bytes-per-channel, etc.
     * @param originalMask found in some "visiting input file".  It is original to the raw mask/chan file.
     * @param channelsData found in the "visiting input file".
     * @param multiMaskId should have the original mask in its list of sub-masks.
     * @return OR-able block of bytes.  Only relevant byte (from maskOffset) will be set.  All others zero.
     */
    @Override
    public byte[] getUpdatedValue(ChannelMetaData channelMetaData, int originalMask, byte[] channelsData, int multiMaskId) {
        assert channelsData.length == channelMetaData.byteCount * channelMetaData.channelCount
                : "Unexpected raw data count " + channelsData.length;
        MultiMaskTracker.MultiMaskBean multiMaskBean = multiMaskTracker.getMultiMaskBean( multiMaskId );
        int maskOffset = multiMaskBean.getMaskOffset( originalMask );
        byte[] rtnVal = new byte[ channelsData.length ];
        if ( maskOffset == -1 ) {
            logger.debug( MASK_MISMATCH_ERROR, multiMaskBean.getMultiMaskNum(), originalMask );
            multiMaskTracker.dumpMaskContents( originalMask );
            return channelsData; // Bypassing.
        }

        // Now, the distillation operation.
        //  For now, we'll look at maximum intensity of any channel.
        if ( channelMetaData.byteCount == 1 ) {
            int maxValue = Integer.MIN_VALUE;
            for ( int i = 0; i < channelMetaData.rawChannelCount; i++ ) {
                if ( channelsData[ i ] > maxValue ) {
                    maxValue = channelsData[ i ];
                }
            }
            rtnVal[ maskOffset ] = (byte)maxValue;
        }
        else {
            int maxValue = ChannelSplitUtils.getMaxValue(channelMetaData, channelsData);

            // Must push the target value above into the returned bytes.
            int startPos = channelMetaData.byteCount * maskOffset;
            for ( int byteInx = 0; byteInx < channelMetaData.byteCount; byteInx++ ) {
                rtnVal[ startPos + byteInx ] = (byte)(0xff & (maxValue >> ( 8 * byteInx )));
            }
        }
        return rtnVal;
    }

}
