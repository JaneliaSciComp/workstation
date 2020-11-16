package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;

import java.util.ArrayList;
import java.util.List;

public class MeshCreateEvent extends ViewerEvent {
    List<TmObjectMesh> meshes = new ArrayList<>();
    public MeshCreateEvent(Object source,
                           List<TmObjectMesh> meshes) {
        super(source);
        this.meshes = meshes;
    }

    public List<TmObjectMesh> getMeshes() {
        return meshes;
    }
}

