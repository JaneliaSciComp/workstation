package org.janelia.workstation.controller.eventbus;

public class ViewerCloseEvent {
    public enum VIEWER {
        HORTA, TASKVIEW, LVV
    };

    private VIEWER viewer;
    public ViewerCloseEvent(VIEWER viewer) {
        this.viewer = viewer;
    }

    public VIEWER getViewer() {
        return viewer;
    }
}

