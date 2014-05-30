package org.janelia.it.workstation.gui.viewer3d.loader;

import org.janelia.it.workstation.gui.viewer3d.VolumeDataAcceptor;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/19/13
 * Time: 4:04 PM
 *
 * Implement this to make a class capable of pushing data into a volume data acceptor.
 */
public interface VolumeLoaderI {
    public enum FileType{
        TIF, LSM, V3DMASK, V3DSIGNAL, MP4, UNKNOWN
    }

    void populateVolumeAcceptor(VolumeDataAcceptor dataAcceptor);
}
