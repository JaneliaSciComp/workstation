package org.janelia.it.workstation.ab2.event;

import java.awt.event.MouseWheelEvent;

public class AB2MouseWheelEvent extends AB2Event {
    private MouseWheelEvent mouseWheelEvent;

    public AB2MouseWheelEvent(MouseWheelEvent e) {
        this.mouseWheelEvent=e;
    }

    public MouseWheelEvent getMouseWheelEvent() {
        return mouseWheelEvent;
    }
}
