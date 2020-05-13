package org.janelia.workstation.controller.eventbus;

public class ViewerEvent {
    protected String sourceClass;
    protected String sourceMethod;

    public String getSourceClass() {
        return sourceClass;
    }

    public String getSourceMethod() {
        return sourceMethod;
    }

    public void setSource(String sourceClass, String sourceMethod) {
        this.sourceClass = sourceClass;
        this.sourceMethod = sourceMethod;
    }
}
