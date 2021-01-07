package org.janelia.workstation.controller.eventbus;

public class ViewerEvent {
    public enum VIEWER {
        HORTA, TASKVIEW, LVV
    };
    protected Object sourceClass;
    protected String sourceMethod;

    public Object getSourceClass() {
        return sourceClass;
    }

    public String getSourceMethod() {
        return sourceMethod;
    }

    public ViewerEvent (Object sourceClass) {
        this.sourceClass = sourceClass;
    }
}
