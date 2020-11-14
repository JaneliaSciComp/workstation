package org.janelia.workstation.controller.eventbus;

public class ViewerOpenEvent {
    public enum VIEWER {
        HORTA, TASKVIEW, LVV
    };

    private VIEWER viewer;
    public ViewerOpenEvent(VIEWER viewer) {
        this.viewer = viewer;
    }

    public VIEWER getViewer() {
        return viewer;
    }
}

