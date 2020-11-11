package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;

public class MeshUpdateEvent {
    TmObjectMesh mesh;
    String oldValue;
    PROPERTY property;
    public enum PROPERTY { PATH, NAME};

    public MeshUpdateEvent(TmObjectMesh mesh, String oldValue, PROPERTY property) {
        this.mesh = mesh;
        this.oldValue = oldValue;
        this.property = property;
    }

    public TmObjectMesh getMesh() {
        return mesh;
    }

    public String getOldValue() {
        return oldValue;
    }

    public PROPERTY getProperty() {
        return property;
    }
}

