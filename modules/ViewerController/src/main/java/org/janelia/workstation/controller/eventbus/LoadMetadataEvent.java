package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

public class LoadMetadataEvent extends LoadEvent {
    boolean isSample;

    public LoadMetadataEvent(Object source,
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
