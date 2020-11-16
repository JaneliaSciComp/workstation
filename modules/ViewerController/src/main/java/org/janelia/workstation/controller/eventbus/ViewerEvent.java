package org.janelia.workstation.controller.eventbus;

public class ViewerEvent {
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
