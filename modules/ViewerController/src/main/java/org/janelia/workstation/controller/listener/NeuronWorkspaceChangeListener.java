package org.janelia.workstation.controller.listener;

import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

/**
 *
 * @author brunsc
 */
public interface NeuronWorkspaceChangeListener {

    void workspaceChanged(TmWorkspace workspace);
    
}
