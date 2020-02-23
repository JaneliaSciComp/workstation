package org.janelia.workstation.gui.large_volume_viewer.listener;

import org.janelia.workstation.gui.large_volume_viewer.TileServer.LoadStatus;

/**
 * Implement this to hear about load status updates.
 * @author fosterl
 */
public interface LoadStatusListener {
    void updateLoadStatus(LoadStatus loadStatus);
}
