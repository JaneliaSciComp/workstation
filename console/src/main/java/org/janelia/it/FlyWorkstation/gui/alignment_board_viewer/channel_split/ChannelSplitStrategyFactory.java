package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.channel_split;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.MultiMaskTracker;
import org.janelia.it.FlyWorkstation.gui.viewer3d.channel_split.ChannelSplitStrategyI;
import org.janelia.it.jacs.compute.access.loader.ChannelMetaData;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/9/13
 * Time: 1:27 PM
 *
 * This returns the appropriate strategy for splitting up channel data as given.
 */
public class ChannelSplitStrategyFactory {

    private ChannelSplitStrategyI[] strategies;
    private MultiMaskTracker multiMaskTracker;
    private ChannelSplitStrategyI trivialStrategy;

    /** Constructor creates the lookup mechanism followed below. */
    public ChannelSplitStrategyFactory( MultiMaskTracker multiMaskTracker ) {
        this.multiMaskTracker = multiMaskTracker;
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
        ChannelSplitStrategyI strategy;
        if ( strategies.length > submaskCount ) {
            strategy = strategies[ submaskCount ];
        }
        else {
            if ( trivialStrategy == null ) {
                trivialStrategy = new ChannelSplitStrategyI() {
                    // This trivial strategy simply preserves the bit value at the maximum depth allowed by
                    // returning an all-zeros byte array.
                    // Lazy-init in case this depth is never reached.
                    @Override
                    public byte[] getUpdatedValue(ChannelMetaData channelMetaDatas, int originalMask, byte[] channelsData, int multiMaskId) {
                        return  new byte[ channelsData.length ];
                    }
                };
            }
            strategy = trivialStrategy;
        }
        return strategy;
    }

    public ChannelSplitStrategyI getStrategyForMask( int maskId ) {
        // First establish what the submask count is.
        int submaskCount = 1;
        if ( multiMaskTracker != null ) {
            submaskCount = multiMaskTracker.getMaskExpansionCount( maskId );
        }
        return getStrategyForSubmaskCount( submaskCount );
    }

}
