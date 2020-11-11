package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;

import java.util.ArrayList;
import java.util.List;

public class MeshDeleteEvent {
    TmObjectMesh mesh;

    public MeshDeleteEvent(TmObjectMesh mesh) {
        this.mesh = mesh;
    }

    public TmObjectMesh getMesh() {
        return mesh;
    }
}

