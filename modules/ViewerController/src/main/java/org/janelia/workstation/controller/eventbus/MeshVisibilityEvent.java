package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;

public class MeshVisibilityEvent {
    TmObjectMesh mesh;
    private final boolean visible;

    public MeshVisibilityEvent(TmObjectMesh mesh, boolean visible) {
        this.mesh = mesh;
        this.visible = visible;
    }

    public TmObjectMesh getMesh() {
        return mesh;
    }

    public boolean isVisible() {
        return visible;
    }
}

