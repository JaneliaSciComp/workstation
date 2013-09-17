package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.channel_split;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.MultiMaskTracker;
import org.janelia.it.FlyWorkstation.gui.viewer3d.channel_split.ChannelSplitStrategyI;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/9/13
 * Time: 1:27 PM
 *
 * This returns the appropriate strategy for splitting up channel data as given.
 */
public class ChannelSplitStrategyFactory {

    ChannelSplitStrategyI[] strategies;

    /** Constructor creates the lookup mechanism followed below. */
    public ChannelSplitStrategyFactory( MultiMaskTracker multiMaskTracker ) {
        strategies = new ChannelSplitStrategyI[ 9 ];
        strategies[ 0 ] = null;
        strategies[ 1 ] = null;
        strategies[ 2 ] = new ByteChannelSplitStrategy( multiMaskTracker );
        strategies[ 3 ] = new ByteChannelSplitStrategy( multiMaskTracker );
        strategies[ 4 ] = new ByteChannelSplitStrategy( multiMaskTracker );
        // 5-8 values may be encoded in bit widths of 4.
        strategies[ 5 ] = new NBitChannelSplitStrategy( multiMaskTracker, 4 );
        strategies[ 6 ] = new NBitChannelSplitStrategy( multiMaskTracker, 4 );
        strategies[ 7 ] = new NBitChannelSplitStrategy( multiMaskTracker, 4 );
        strategies[ 8 ] = new NBitChannelSplitStrategy( multiMaskTracker, 4 );
    }

    public ChannelSplitStrategyI getStrategyForSubmaskCount( int submaskCount ) {
        return strategies[ submaskCount ];
    }

}
