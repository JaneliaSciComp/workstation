package org.janelia.it.workstation.ab2.event;

import org.janelia.it.workstation.browser.model.SampleImage;

public class AB2SampleAddedEvent extends AB2Event {
    
    private SampleImage sampleImage;

    public AB2SampleAddedEvent(SampleImage sampleImage) {
        this.sampleImage=sampleImage;
    }

    public SampleImage getSampleImage() {
        return sampleImage;
    }

}
