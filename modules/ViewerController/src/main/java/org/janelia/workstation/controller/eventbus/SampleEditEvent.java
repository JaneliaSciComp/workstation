package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmSample;

public class SampleEditEvent extends SampleEvent {

    public SampleEditEvent(TmSample sample, Long sampleId) {
        super(sample, sampleId);
    }
}
