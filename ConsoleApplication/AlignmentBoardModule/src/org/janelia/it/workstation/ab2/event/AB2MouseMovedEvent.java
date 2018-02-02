package org.janelia.it.workstation.ab2.event;

import java.awt.event.MouseEvent;

public class AB2MouseMovedEvent extends AB2MouseEvent {

    public AB2MouseMovedEvent(MouseEvent e) {
        this.mouseEvent=e;
    }

}
