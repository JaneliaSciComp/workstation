package org.janelia.it.FlyWorkstation.gui.viewer3d.loader;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.MultiMaskTracker;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.VolumeDataI;
import org.janelia.it.FlyWorkstation.shared.annotations.NotThreadSafe;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 8/28/13
 * Time: 2:12 PM
 *
 * This can change the numbers of masks, and if it does, send these new masks off to be tracked.
 */
public class RemaskingAcceptorDecorator extends AbstractAcceptorDecorator {
    private VolumeDataI maskVolumeData;
    private int pixelByteCount;
    private boolean binary;
    private MultiMaskTracker multiMaskTracker;

    public RemaskingAcceptorDecorator(
            MaskChanDataAcceptorI wrappedAcceptor,
            MultiMaskTracker multiMaskTracker,
            VolumeDataI maskVolumeData,
            int pixelByteCount,
            boolean binary
    ) {
        this.setWrappedAcceptor( wrappedAcceptor );
        this.maskVolumeData = maskVolumeData;
        this.pixelByteCount = pixelByteCount;
        this.multiMaskTracker = multiMaskTracker;
        this.binary = binary;
    }

    /** Pass through. */
    @Override
    public int addChannelData(
            byte[] channelData, long position, long x, long y, long z, ChannelMetaData channelMetaData
    ) throws Exception {
        return wrappedAcceptor.addChannelData( channelData, position, x, y, z, channelMetaData );
    }

    /**
     * Figure out if the mask is already in use.
     *
     * @param maskNumber what mask associated with the current use of this slot.
     * @param position convenience: where the mask goes in linear, 1D-array, coords.
     * @param x convenience: an alternative; x in 3D cords.
     * @param y in 3D
     * @param z in 3D
     * @return number added.  Either 1 or 0.
     * @throws Exception thrown by called methods.
     */
    @NotThreadSafe(
            why = "Data read, tested, written sans 'synchronized'.  Multiple renderables may write same x,y,z"
    )
    @Override
    public int addMaskData(Integer maskNumber, long position, long x, long y, long z) throws Exception {
        Integer finalMaskNumber = maskNumber;

        if ( ! binary ) {
            int oldVolumeMask = 0;
            byte[] volumeData = this.maskVolumeData.getCurrentVolumeData();
            if ( volumeData != null ) {
                // Assumed little-endian.
                for ( int j = 0; j < pixelByteCount; j++ ) {
                    int volumeLoc = j + ((int) position * pixelByteCount);
                    // Here enforced: need to take previous mask into account.
                    oldVolumeMask += volumeData[ volumeLoc ] << (8*j);
                }

                // Got old mask.  Need to make changes?
                if ( oldVolumeMask != 0 ) {
                    // The handoff.
                    finalMaskNumber = multiMaskTracker.getMask( maskNumber, oldVolumeMask );
                }
            }
            else {
                throw new RuntimeException("No volume data available.  Cannot add mask data.");
            }
        }

        return wrappedAcceptor.addMaskData( finalMaskNumber, position, x, y, z );

    }
}
