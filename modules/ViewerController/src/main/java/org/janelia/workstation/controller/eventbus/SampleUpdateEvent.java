package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmSample;

public class SampleUpdateEvent extends SampleEvent {
    public SampleUpdateEvent(Object source, TmSample sample, Long sampleId) {
        super(source, sample, sampleId);
    }
}
