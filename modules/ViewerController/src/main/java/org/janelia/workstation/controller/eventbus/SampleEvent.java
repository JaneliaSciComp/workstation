package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmSample;

public class SampleEvent {
    public enum Type {
      CREATE, DELETE, LOAD, EDIT, UPDATE;
    };
    private Type type;
    private long sampleId;
    private TmSample sample;

    public Type getEventType() {
        return type;
    }
    public void setEventType(Type type) {
        this.type = type;
    }

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
