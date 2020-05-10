package org.janelia.workstation.controller.eventbus;

public class LoadProjectEvent extends LoadEvent {
    boolean isSample = false;

    public LoadProjectEvent(boolean isSample) {
        this.isSample = isSample;
    }

    public boolean isSample() {
        return isSample;
    }

    public void setSample(boolean sample) {
        isSample = sample;
    }
}
