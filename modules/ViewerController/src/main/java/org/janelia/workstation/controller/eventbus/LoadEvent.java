package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class LoadEvent  extends ViewerEvent {
    TmWorkspace workspace;
    TmSample sample;

    public LoadEvent(Object source, TmWorkspace workspace, TmSample sample) {
        super(source);
        this.workspace = workspace;
        this.sample = sample;
    }

    public TmWorkspace getWorkspace() {
        return workspace;
    }
    public TmSample getSample() {
        return sample;
    }
}
