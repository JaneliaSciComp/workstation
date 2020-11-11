package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class WorkspaceDeleteEvent extends WorkspaceEvent{
    public WorkspaceDeleteEvent(TmWorkspace workspace, Long workspaceId) {
        super(workspace, workspaceId);
    }
}
