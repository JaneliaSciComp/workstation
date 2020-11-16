package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmSample;

public class SampleEditEvent extends SampleEvent {

    public SampleEditEvent(Object source, TmSample sample, Long sampleId) {
        super(source, sample, sampleId);
    }
}
