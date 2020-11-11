package org.janelia.workstation.gui.large_volume_viewer.listener;

import org.janelia.workstation.geom.Vec3;

/**
 * Implement this to be told when the camera-pan should be done.
 *
 * @author fosterl
 */
public interface CameraPanToListener {
    void cameraPanTo(Vec3 location);
}
