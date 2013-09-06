package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/6/13
 * Time: 10:32 AM
 *
 * This is a source interface for volume data bytes.
 */
public interface VolumeDataI {
    /**
     * Call this to get the volume data.  Do not cache this data, as it is meant to be dynamically
     * available/generated for use.  Also the implementation may use or enforce an order-of-calls
     * dependency.  The return value may be null.
     *
     * @return bytes for all volume data currently available.
     */
    byte[] getCurrentVolumeData();
}
