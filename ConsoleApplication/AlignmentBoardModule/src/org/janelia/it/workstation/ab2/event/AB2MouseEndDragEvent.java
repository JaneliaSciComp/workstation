package org.janelia.it.workstation.ab2.event;

import java.awt.event.MouseEvent;

public class AB2MouseEndDragEvent extends AB2MouseEvent {
    private Object sourceObject;

    public AB2MouseEndDragEvent(MouseEvent e, Object sourceObject) {
        this.mouseEvent=e;
        this.sourceObject=sourceObject;
    }

    public Object getSourceObject() { return sourceObject; }
}
