package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;

import java.util.ArrayList;
import java.util.List;

public class MeshUpdateEvent {
    public List<TmObjectMesh> getMeshes() {
        return meshes;
    }

    public void setMeshes(List<TmObjectMesh> meshes) {
        this.meshes = meshes;
    }

    List<TmObjectMesh> meshes = new ArrayList<>();
}

