package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class WorkspaceEvent extends ViewerEvent {
    private long WorkspaceId;
    private TmWorkspace Workspace;

    public long getWorkspaceId() {
        return WorkspaceId;
    }

    public void setWorkspaceId(long WorkspaceId) {
        this.WorkspaceId = WorkspaceId;
    }

    public TmWorkspace getWorkspace() {
        return Workspace;
    }

    public void setWorkspace(TmWorkspace Workspace) {
        this.Workspace = Workspace;
    }
}
