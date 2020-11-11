package org.janelia.workstation.controller.listener;

/**
 * Implement this to hear about when some queue has been drained.
 * 
 * @author fosterl
 */
public interface StatusUpdateListener {
    void update();
}
