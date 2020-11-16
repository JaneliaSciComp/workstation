package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmSample;

public class SampleDeleteEvent extends SampleEvent {

    public SampleDeleteEvent(Object source,
                             TmSample sample, Long sampleId) {
        super(source, sample, sampleId);
    }
}
