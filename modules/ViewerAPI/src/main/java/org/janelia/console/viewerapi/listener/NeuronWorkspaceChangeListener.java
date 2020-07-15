package org.janelia.console.viewerapi.listener;

import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

/**
 *
 * @author brunsc
 */
public interface NeuronWorkspaceChangeListener {

    void workspaceChanged(TmWorkspace workspace);
    
}
