package org.janelia.it.workstation.ab2.event;

import org.janelia.it.jacs.model.domain.sample.Sample;

public class AB2SampleAddedEvent extends AB2Event {
    Sample sample;

    public AB2SampleAddedEvent(Sample sample) {
        this.sample=sample;
    }

    public Sample getSample() { return sample; }
}
