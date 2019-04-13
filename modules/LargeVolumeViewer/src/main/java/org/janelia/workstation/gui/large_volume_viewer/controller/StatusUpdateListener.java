package org.janelia.workstation.gui.large_volume_viewer.controller;

/**
 * Implement this to hear about when some queue has been drained.
 * 
 * @author fosterl
 */
public interface StatusUpdateListener {
    void update();
}
