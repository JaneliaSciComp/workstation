package org.janelia.workstation.controller.eventbus;

public class UnloadProjectEvent extends LoadEvent {
    boolean isSample = false;

    public UnloadProjectEvent(boolean isSample) {
        this.isSample = isSample;
    }

    public boolean isSample() {
        return isSample;
    }

    public void setSample(boolean sample) {
        isSample = sample;
    }
}
