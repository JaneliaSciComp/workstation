package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class WorkspaceCreateEvent extends WorkspaceEvent{
    public WorkspaceCreateEvent(Object source,
                                TmWorkspace workspace, Long workspaceId) {
        super(source, workspace, workspaceId);
    }
}
