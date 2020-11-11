package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class WorkspaceEvent extends ViewerEvent {
    private long workspaceId;
    private TmWorkspace workspace;

    public WorkspaceEvent(TmWorkspace workspace, Long workspaceId) {
        this.workspaceId = workspaceId;
        this.workspace = workspace;
    }

    public long getWorkspaceId() {
        return workspaceId;
    }
    public TmWorkspace getWorkspace() {
        return workspace;
    }
}
