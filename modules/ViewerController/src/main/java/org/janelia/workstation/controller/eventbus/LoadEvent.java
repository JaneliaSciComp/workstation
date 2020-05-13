package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class LoadEvent  extends ViewerEvent {
    TmWorkspace workspace;
    TmSample sample;

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
