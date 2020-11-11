package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class LoadProjectEvent extends LoadEvent {
    boolean isSample;

    public LoadProjectEvent(TmWorkspace workspace, TmSample sample, boolean isSample) {
        super(workspace, sample);
        this.isSample = isSample;
    }

    public boolean isSample() {
        return isSample;
    }

}
