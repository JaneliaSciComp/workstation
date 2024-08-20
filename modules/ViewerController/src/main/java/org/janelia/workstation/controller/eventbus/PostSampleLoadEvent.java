package org.janelia.workstation.controller.eventbus;

public class PostSampleLoadEvent extends ViewerEvent {
    private boolean sample = false;
    private Object project;

    public PostSampleLoadEvent(Object project,
                               boolean isSample) {
        super(project);
        this.project = project;
        this.sample = isSample;
    }

    public boolean isSample() {
        return sample;
    }
    public Object getProject() {
        return project;
    }

}
