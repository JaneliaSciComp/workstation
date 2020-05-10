package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmSample;

public class SampleEvent {
    private long sampleId;
    private TmSample sample;

    public long getSampleId() {
        return sampleId;
    }

    public void setSampleId(long sampleId) {
        this.sampleId = sampleId;
    }

    public TmSample getSample() {
        return sample;
    }

    public void setSample(TmSample sample) {
        this.sample = sample;
    }
}
