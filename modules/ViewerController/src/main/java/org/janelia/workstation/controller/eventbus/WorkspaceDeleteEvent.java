package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class WorkspaceDeleteEvent extends WorkspaceEvent{
    public WorkspaceDeleteEvent(Object source, TmWorkspace workspace, Long workspaceId) {
        super(source,workspace, workspaceId);
    }
}
