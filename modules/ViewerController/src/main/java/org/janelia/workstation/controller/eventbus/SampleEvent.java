package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmSample;

public class SampleEvent extends ViewerEvent {
    private Long sampleId;
    private TmSample sample;

    public SampleEvent(Object source, TmSample sample, Long sampleId) {
        super(source);
        this.sampleId = sampleId;
        this.sample = sample;
    }

    public long getSampleId() {
        return sampleId;
    }
    public TmSample getSample() {
        return sample;
    }
}
