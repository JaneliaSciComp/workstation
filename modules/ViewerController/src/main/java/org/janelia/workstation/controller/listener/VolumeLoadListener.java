package org.janelia.workstation.controller.listener;

import java.net.URL;

/**
 * Implement this to hear about volume URLs being loaded.
 *
 * @author fosterl
 */
public interface VolumeLoadListener {
    void volumeLoaded(URL volumeURI);
}
