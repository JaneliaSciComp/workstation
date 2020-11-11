package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class LoadImageryEvent extends LoadEvent {
    public LoadImageryEvent(TmWorkspace workspace, TmSample sample) {
        super(workspace, sample);
    }
}
