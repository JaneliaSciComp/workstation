package org.janelia.workstation.controller.eventbus;

public class LoadMetadataEvent extends LoadEvent {
    boolean isSample;

    public LoadMetadataEvent() {
        this.isSample = false;
    }

    public boolean isSample() {
        return isSample;
    }

    public void setSample(boolean sample) {
        isSample = sample;
    }
}
