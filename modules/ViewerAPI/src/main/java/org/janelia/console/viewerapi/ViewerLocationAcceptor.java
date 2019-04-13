package org.janelia.console.viewerapi;

import java.net.URL;

/**
 * Implement this to have your viewer accept some location as its own.
 * @author fosterl
 */
public interface ViewerLocationAcceptor {
    void acceptLocation(SampleLocation sampleLocation) throws Exception;
}
