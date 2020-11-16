package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmSample;

public class SampleCreateEvent extends SampleEvent {

    public SampleCreateEvent(Object source,
                             TmSample sample, Long sampleId) {
        super(source, sample, sampleId);
    }
}
