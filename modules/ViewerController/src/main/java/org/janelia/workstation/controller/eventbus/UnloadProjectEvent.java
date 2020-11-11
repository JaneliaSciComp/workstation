package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class UnloadProjectEvent extends LoadEvent {
    boolean isSample = false;

    public UnloadProjectEvent(TmWorkspace workspace, TmSample sample,
                              boolean isSample) {
        super(workspace, sample);
        this.isSample = isSample;
    }

    public boolean isSample() {
        return isSample;
    }
}
