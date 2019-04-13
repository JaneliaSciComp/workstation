package org.janelia.workstation.integration.api;

import java.util.concurrent.Callable;

/**
 * Service for interacting with the Workstation browsing GUI, including the explorer, inspector, and other
 * related components.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface BrowsingController {

    void refreshExplorer();

    void refreshExplorer(final Callable<Void> success);

}
