package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class LoadNeuronsEvent extends LoadEvent {
    public LoadNeuronsEvent(TmWorkspace workspace, TmSample sample) {
        super(workspace, sample);
    }
    // same info required as LoadEvent
}
