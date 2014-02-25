package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.channel_split;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.MultiMaskTracker;
import org.janelia.it.FlyWorkstation.gui.alignment_board.channel_split.ChannelSplitStrategyI;
import org.janelia.it.FlyWorkstation.gui.alignment_board.loader.ChannelMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/9/13
 * Time: 3:18 PM
 *
 * Implements a channel split, by giving four bits to each channel-data contender.  There is a quiet assumption here,
 * that the input byte count is four.
 */
public class NBitChannelSplitStrategy implements ChannelSplitStrategyI {
    private int outputValWidth = 4;
    private MultiMaskTracker multiMaskTracker;
    private Logger logger = LoggerFactory.getLogger( NBitChannelSplitStrategy.class );

    public NBitChannelSplitStrategy(MultiMaskTracker multiMaskTracker, int bitWidth) {
        assert bitWidth < 8 : "Bit width " + bitWidth+" should be < 8, or this strategy is inappropriate.";
        assert bitWidth == 2 || bitWidth == 4 : "Bit width " + bitWidth+" should be a power of 2.";

        this.multiMaskTracker = multiMaskTracker;
        this.outputValWidth = bitWidth;

    }

    /**
     * Takes original channel data from a file read, and shrinks the number of bits representing the incoming
     * channel data into the bit width given for this strategy object.
     */
    @Override
    public byte[] getUpdatedValue(ChannelMetaData channelMetaData, int originalMask, byte[] channelsData, int multiMaskId) {
        assert channelsData.length == channelMetaData.byteCount * channelMetaData.channelCount
                : "Unexpected raw data count " + channelsData.length;

        int totalAccessibleBits = channelMetaData.byteCount * channelMetaData.channelCount * 8;
        int effectiveBitWidth = outputValWidth * channelMetaData.byteCount;
        if ( totalAccessibleBits % effectiveBitWidth != 0 ) {
            throw new RuntimeException("Total width for split " + totalAccessibleBits + " not divisible by " + outputValWidth );
        }
        byte[] rtnVal = new byte[ channelsData.length ];

        // Now, the distillation operation.
        //  For now, we'll look at maximum intensity of any channel.
        int maxChannelValue = ChannelSplitUtils.getMaxValue( channelMetaData, channelsData );

        // Must push the target value above into the returned bytes.

        // Example: max input for 2-byte channels would be 2^16.
        //  Although the total bits will be larger, we are downsampling only a single channel--the
        //  highest-intensity channel.
        int powByteCount = (int) Math.pow(2, 8 * channelMetaData.byteCount);

        // Example: all-1's bit masking value for 2-byte channels would be 16 1's (or FFFF).
        int powEffectiveBits = (int) Math.pow(2, effectiveBitWidth);
        int bitWidthMaskingValue = powEffectiveBits - 1;
        double bitWidthCompressionFactor = (double)powEffectiveBits / (double)powByteCount;
        int compressedValue = (int)Math.ceil((double) maxChannelValue * bitWidthCompressionFactor);
        compressedValue = compressedValue > bitWidthMaskingValue ? bitWidthMaskingValue : compressedValue;

        logger.debug(
                "Found max channel value of " + maxChannelValue + " and compressed value of " + compressedValue +
                        " and compression factor of " + bitWidthCompressionFactor
        );

        MultiMaskTracker.MultiMaskBean multiMaskBean = multiMaskTracker.getMultiMaskBean(multiMaskId);

        // Which priority submask are we using?
        int maskOffset = multiMaskBean.getMaskOffset(originalMask);
        if ( maskOffset < 0 ) {
            logger.debug( MASK_MISMATCH_ERROR, multiMaskBean.getMultiMaskNum(), originalMask );
            multiMaskTracker.dumpMaskContents( originalMask );
            // Bypassing...
        }
        else {
            int bitOffset = maskOffset * effectiveBitWidth;
            int byteStartPos = bitOffset / 8;
            int intraByteStartPos = (effectiveBitWidth * maskOffset) % 8;
            rtnVal[ byteStartPos ] = (byte)((bitWidthMaskingValue & compressedValue) << intraByteStartPos);

        }

        return rtnVal;
    }
}
