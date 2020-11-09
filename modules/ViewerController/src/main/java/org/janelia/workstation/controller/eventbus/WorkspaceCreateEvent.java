package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class WorkspaceCreateEvent extends WorkspaceEvent{
    public WorkspaceCreateEvent(TmWorkspace workspace, Long workspaceId) {
        super(workspace, workspaceId);
    }
}
