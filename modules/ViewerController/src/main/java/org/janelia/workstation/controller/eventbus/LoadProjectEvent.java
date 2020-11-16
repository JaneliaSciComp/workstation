package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class LoadProjectEvent extends LoadEvent {
    boolean isSample;

    public LoadProjectEvent(Object source,
                            TmWorkspace workspace,
                            TmSample sample,
                            boolean isSample) {
        super(source,workspace, sample);
        this.isSample = isSample;
    }

    public boolean isSample() {
        return isSample;
    }

}
