package org.janelia.it.workstation.browser.nodes;

import org.janelia.model.domain.interfaces.HasIdentifier;

/**
 * Marker interface for root nodes.
 */
public interface RootNode extends HasIdentifier {

    @Override
    default Long getId() {
        return 0L;
    }
}
