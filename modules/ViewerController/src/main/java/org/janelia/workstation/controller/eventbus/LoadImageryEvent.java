package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class LoadImageryEvent extends LoadEvent {
    public LoadImageryEvent(Object source, TmWorkspace workspace, TmSample sample) {
        super(source, workspace, sample);
    }
}
