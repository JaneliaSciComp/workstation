package org.janelia.workstation.gui.large_volume_viewer.listener;

import java.net.URL;

/**
 * Implement this to hear about when URLs are loaded.
 * 
 * @author fosterl
 */
public interface UrlLoadListener {
    void loadUrl(URL url);
}
