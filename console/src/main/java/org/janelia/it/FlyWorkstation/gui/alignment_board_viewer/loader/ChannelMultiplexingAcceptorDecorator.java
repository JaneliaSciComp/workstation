package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.loader;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.MultiMaskTracker;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.VolumeDataI;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.channel_split.ChannelSplitStrategyFactory;
import org.janelia.it.FlyWorkstation.gui.viewer3d.channel_split.ChannelSplitStrategyI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.AbstractAcceptorDecorator;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.ChannelMetaData;
import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.MaskChanDataAcceptorI;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/9/13
 * Time: 12:35 PM
 *
 * This acceptor-decorator attempts to subdivide the channel data among all the masks "present" (in line??) for the
 * given voxel position.  It will consult the multi-mask tracker, using the content already set in the voxel's
 * "mask texture slot" to work out exactly how to divide the "signal bandwidth" among all the different sub-masks.
 */
public class ChannelMultiplexingAcceptorDecorator extends AbstractAcceptorDecorator {
    private VolumeDataI wholeMaskVolume;
    private MultiMaskTracker multiMaskTracker;
    private int maskByteCount;

    private ChannelSplitStrategyFactory channelSplitStrategyFactory;

    /**
     * Configures this class with all callable instances needed to properly build out the channel data.
     *
     * @param multiMaskTracker shall have been populated with multi-to-single-list mapping prior to calling this.
     * @param wholeMaskVolume shall have been fully populated with multi and single masks prior to calling this.
     * @param wrappedAcceptor will receive the modified channel bytes for each voxel.
     */
    public ChannelMultiplexingAcceptorDecorator(
            MultiMaskTracker multiMaskTracker,
            VolumeDataI wholeMaskVolume,
            MaskChanDataAcceptorI wrappedAcceptor,
            int maskByteCount
    ) {
        this.multiMaskTracker = multiMaskTracker;
        this.wholeMaskVolume = wholeMaskVolume;
        this.wrappedAcceptor = wrappedAcceptor;
        this.maskByteCount = maskByteCount;

        channelSplitStrategyFactory = new ChannelSplitStrategyFactory( multiMaskTracker );
    }

    /**
     * Multiplexes the current mask's contribution into its appropriate slot in the
     * channel data.
     *
     * @param originalMask mask target for renderable.
     * @param channelData data found in renderable's channel input file.
     * @param position the 1-D array position.
     * @param x y,z the coords in 3D
     * @param channelMetaData all about the renderable's input characteristics.
     * @return number of effective positions handled.  Should be 1. 0 -> error.
     * @throws Exception by called methods, or if volume data not initialized.
     */
    @Override
    public int addChannelData(
            Integer originalMask,
            byte[] channelData,
            long position,
            long x, long y, long z,
            ChannelMetaData channelMetaData
    ) throws Exception {
        int rtnVal;

        // Find the appropriate slot in the mask data, and get its value.
        byte[] maskVolumeData = this.wholeMaskVolume.getCurrentVolumeData();
        int volumeMask = 0;
        if ( maskVolumeData != null ) {
            int volumeLoc = ((int) position * maskByteCount);
            // Assumed little-endian.
            for ( int j = 0; j < maskByteCount; j++ ) {
                // The volume mask is the one currently in use.  This could be a single or multi-mask.
                volumeMask += maskVolumeData[ volumeLoc ] << (8*j);
                volumeLoc++;
            }

            int voxelStackCount = multiMaskTracker.getMaskExpansionCount( volumeMask );
            if ( voxelStackCount > 1 ) {
                // Given that mask number represents a multi-mask, work out what strategy to use. Strategy will
                // pare down the bits from maximum to some smaller number.
                //
                ChannelSplitStrategyI strategy = channelSplitStrategyFactory.getStrategyForSubmaskCount( voxelStackCount );
                byte[] updatedChannelData = strategy.updateValue(channelMetaData, originalMask, channelData, volumeMask);
                rtnVal = wrappedAcceptor.addChannelData( originalMask, updatedChannelData, position, x, y, z, channelMetaData );
            }
            else {
                rtnVal = wrappedAcceptor.addChannelData( originalMask, channelData, position, x, y, z, channelMetaData );
            }

        }
        else {
            throw new RuntimeException("No volume data available.  Cannot add mask data.");
        }

        return rtnVal;
    }

    @Override
    public int addMaskData(Integer maskNumber, long position, long x, long y, long z) throws Exception {
        // This is actually unlikely to be called, but should pass through in any event.
        return wrappedAcceptor.addMaskData( maskNumber, position, x, y, z );
    }

}
