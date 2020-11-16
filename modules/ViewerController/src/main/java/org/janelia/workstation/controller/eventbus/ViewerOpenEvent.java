package org.janelia.workstation.controller.eventbus;

public class ViewerOpenEvent extends ViewerEvent {
    public enum VIEWER {
        HORTA, TASKVIEW, LVV
    };

    private VIEWER viewer;
    public ViewerOpenEvent(Object source, VIEWER viewer) {
        super(source);
        this.viewer = viewer;
    }

    public VIEWER getViewer() {
        return viewer;
    }
}

