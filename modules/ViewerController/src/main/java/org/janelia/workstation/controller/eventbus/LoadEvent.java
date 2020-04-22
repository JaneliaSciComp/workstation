package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class LoadEvent {
    public enum Type {
        IMAGERY_COMPLETE, METADATA_COMPLETE, PROJECT_COMPLETE;
    };
    private Type type;
    TmWorkspace workspace;
    TmSample sample;

    public LoadEvent(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }


    public TmWorkspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(TmWorkspace workspace) {
        this.workspace = workspace;
    }

    public TmSample getSample() {
        return sample;
    }

    public void setSample(TmSample sample) {
        this.sample = sample;
    }

}
