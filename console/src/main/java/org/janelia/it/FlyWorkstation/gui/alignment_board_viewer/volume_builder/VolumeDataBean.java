package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.VolumeDataI;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/19/13
 * Time: 1:20 PM
 *
 * This is a simplistic "bean" implementation of a volume data object.  Just allocate and set bytes.
 */
public class VolumeDataBean implements VolumeDataI {

    private byte[] cachedVolume;

    public VolumeDataBean( byte[] wholeVolume ) {
        cachedVolume = wholeVolume;
    }

    public VolumeDataBean( long size ) {
        cachedVolume = new byte[ (int)size ];
    }

    @Override
    public boolean isVolumeAvailable() {
        return true;
    }

    @Override
    public byte[] getCurrentVolumeData() {
        return cachedVolume;
    }

    @Override
    public byte getCurrentValue(long location) {
        return cachedVolume[ (int)location ];
    }

    @Override
    public void setCurrentValue(long location, byte value) {
        cachedVolume[ (int)location ] = value;
    }

    @Override
    public long length() {
        return cachedVolume.length;
    }
}
