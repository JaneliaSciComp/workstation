package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;

public class MeshUpdateEvent extends ViewerEvent {
    TmObjectMesh mesh;
    String oldValue;
    PROPERTY property;
    public enum PROPERTY { PATH, NAME};

    public MeshUpdateEvent(Object source,
                           TmObjectMesh mesh,
                           String oldValue,
                           PROPERTY property) {
        super(source);
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

