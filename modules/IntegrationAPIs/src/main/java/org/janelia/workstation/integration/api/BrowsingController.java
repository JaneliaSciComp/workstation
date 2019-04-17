package org.janelia.workstation.integration.api;

import java.util.List;
import java.util.concurrent.Callable;

import org.janelia.model.domain.Reference;

/**
 * Service for interacting with the Workstation browsing GUI, including the explorer, inspector, and other
 * related components.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface BrowsingController {

    /**
     * Refresh the data explorer.
     */
    void refreshExplorer();

    /**
     * Refresh the data explorer and call the given callback when complete.
     * @param success callback if refresh is successful
     */
    void refreshExplorer(final Callable<Void> success);

    /**
     * Retrieve the history of recently opened objects.
     * @return
     */
    List<Reference> getRecentlyOpenedHistory();

    /**
     * Update the history to add the given object reference to the top of the stack.
     * @param ref
     */
    void updateRecentlyOpenedHistory(Reference ref);

}
