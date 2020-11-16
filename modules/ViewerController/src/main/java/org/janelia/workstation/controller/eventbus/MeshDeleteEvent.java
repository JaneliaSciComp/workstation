package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;

import java.util.ArrayList;
import java.util.List;

public class MeshDeleteEvent extends ViewerEvent {
    TmObjectMesh mesh;

    public MeshDeleteEvent(Object source,
                           TmObjectMesh mesh) {
        super(source);
        this.mesh = mesh;
    }

    public TmObjectMesh getMesh() {
        return mesh;
    }
}

