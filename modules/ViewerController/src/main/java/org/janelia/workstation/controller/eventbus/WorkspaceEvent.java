package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class WorkspaceEvent {
    public enum Type {
        CREATE, LOAD, DELETE, IMPORT, EDIT, UPDATE, CLEAR;
    };
    private WorkspaceEvent.Type type;
    private long WorkspaceId;
    private TmWorkspace Workspace;

    public WorkspaceEvent() {
    }

    public WorkspaceEvent.Type getEventType() {
        return type;
    }
    public void setEventType(WorkspaceEvent.Type type) {
        this.type = type;
    }

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
