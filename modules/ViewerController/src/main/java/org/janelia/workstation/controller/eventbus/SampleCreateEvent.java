package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmSample;

public class SampleCreateEvent extends SampleEvent {

    public SampleCreateEvent(TmSample sample, Long sampleId) {
        super(sample, sampleId);
    }
}
