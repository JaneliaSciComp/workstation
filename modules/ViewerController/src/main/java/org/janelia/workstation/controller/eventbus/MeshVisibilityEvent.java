package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;

public class MeshVisibilityEvent extends ViewerEvent {
    TmObjectMesh mesh;
    private final boolean visible;

    public MeshVisibilityEvent(Object source,
                               TmObjectMesh mesh,
                               boolean visible) {
        super(source);
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

