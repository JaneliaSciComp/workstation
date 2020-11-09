package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;

import java.util.ArrayList;
import java.util.List;

public class MeshCreateEvent {
    List<TmObjectMesh> meshes = new ArrayList<>();
    public MeshCreateEvent(List<TmObjectMesh> meshes) {
        this.meshes = meshes;
    }

    public List<TmObjectMesh> getMeshes() {
        return meshes;
    }
}

