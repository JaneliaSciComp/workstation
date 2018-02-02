package org.janelia.it.workstation.ab2.event;

public class AB2ChangeModeEvent extends AB2Event {
    private Class newMode;

    public AB2ChangeModeEvent(Class newMode) {
        this.newMode=newMode;
    }

    public Class getNewMode() { return newMode; }
}
