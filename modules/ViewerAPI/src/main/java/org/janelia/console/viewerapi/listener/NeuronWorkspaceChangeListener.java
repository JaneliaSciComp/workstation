package org.janelia.console.viewerapi.listener;

import org.janelia.console.viewerapi.model.NeuronSet;

/**
 *
 * @author brunsc
 */
public interface NeuronWorkspaceChangeListener {

    void workspaceChanged(NeuronSet workspace);
    
}
