package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class UnloadProjectEvent extends LoadEvent {
    boolean isSample = false;

    public UnloadProjectEvent(Object source,
                              TmWorkspace workspace,
                              TmSample sample,
                              boolean isSample) {
        super(source, workspace, sample);
        this.isSample = isSample;
    }

    public boolean isSample() {
        return isSample;
    }
}
