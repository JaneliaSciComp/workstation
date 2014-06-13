package org.janelia.it.workstation.gui.alignment_board.channel_split;

import org.janelia.it.workstation.gui.alignment_board.loader.ChannelMetaData;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/9/13
 * Time: 1:18 PM
 *
 * Implement this to create some way of collapsing the channel data into the right-sized slots and placing the data
 * appropriately into the channel data.  Channel data will be modified (it is an output).
 */
public interface ChannelSplitStrategyI {
    String MASK_MISMATCH_ERROR = "Mismatch between masks and channels.  Seeing multimask {}, which does not contain {}.";

    byte[] getUpdatedValue(ChannelMetaData channelMetaDatas, int originalMask, byte[] channelsData, int multiMaskId);
}
