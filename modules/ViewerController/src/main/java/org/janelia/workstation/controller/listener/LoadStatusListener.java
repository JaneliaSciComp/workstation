package org.janelia.workstation.controller.listener;

import org.janelia.workstation.controller.tileimagery.TileServer.LoadStatus;

/**
 * Implement this to hear about load status updates.
 * @author fosterl
 */
public interface LoadStatusListener {
    void updateLoadStatus(LoadStatus loadStatus);
}
