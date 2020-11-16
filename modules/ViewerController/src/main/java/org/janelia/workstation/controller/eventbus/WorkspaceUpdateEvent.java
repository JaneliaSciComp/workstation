package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class WorkspaceUpdateEvent extends WorkspaceEvent{
    public WorkspaceUpdateEvent(Object source,
                                TmWorkspace workspace, Long workspaceId) {
        super(source, workspace, workspaceId);
    }
}
