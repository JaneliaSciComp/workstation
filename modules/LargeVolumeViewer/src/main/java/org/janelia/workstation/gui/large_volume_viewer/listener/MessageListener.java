package org.janelia.workstation.gui.large_volume_viewer.listener;

/**
 * Implement this to hear about a generic message change/update.
 * @author fosterl
 */
public interface MessageListener {
    void message(String msg);
}
