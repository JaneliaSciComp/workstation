package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class WorkspaceUpdateEvent extends WorkspaceEvent{
    public WorkspaceUpdateEvent(TmWorkspace workspace, Long workspaceId) {
        super(workspace, workspaceId);
    }
}
