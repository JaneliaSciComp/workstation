package org.janelia.it.workstation.ab2.event;

import java.awt.event.MouseEvent;

public class AB2MouseExitedEvent extends AB2MouseEvent {

    public AB2MouseExitedEvent(MouseEvent e) {
        this.mouseEvent=e;
    }

}
