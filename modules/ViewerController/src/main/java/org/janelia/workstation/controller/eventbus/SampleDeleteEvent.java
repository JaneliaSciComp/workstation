package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmSample;

public class SampleDeleteEvent extends SampleEvent {

    public SampleDeleteEvent(TmSample sample, Long sampleId) {
        super(sample, sampleId);
    }
}
