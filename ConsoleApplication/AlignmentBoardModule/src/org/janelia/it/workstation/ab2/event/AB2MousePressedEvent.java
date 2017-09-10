package org.janelia.it.workstation.ab2.event;

import java.awt.event.MouseEvent;

public class AB2MousePressedEvent extends AB2Event {
    private MouseEvent mouseEvent;

    public AB2MousePressedEvent(MouseEvent e) {
        this.mouseEvent=e;
    }

    public MouseEvent getMouseEvent() {
        return mouseEvent;
    }
}
