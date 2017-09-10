package org.janelia.it.workstation.ab2.event;

import java.awt.event.MouseEvent;

public class AB2MouseDraggedEvent extends AB2Event {
    private MouseEvent mouseEvent;

    public AB2MouseDraggedEvent(MouseEvent e) {
        this.mouseEvent=e;
    }

    public MouseEvent getMouseEvent() {
        return mouseEvent;
    }
}
