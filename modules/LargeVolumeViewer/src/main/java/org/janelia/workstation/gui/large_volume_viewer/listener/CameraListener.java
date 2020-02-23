package org.janelia.workstation.gui.large_volume_viewer.listener;

import org.janelia.it.jacs.shared.geom.Vec3;

/**
 * Implement this to hear about camera-related events.
 * @author fosterl
 */
public interface CameraListener {
    void viewChanged();
    void zoomChanged(Double zoom);
    void focusChanged(Vec3 focus);
}
