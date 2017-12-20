package org.janelia.it.workstation.ab2.event;

import java.awt.event.MouseEvent;

public class AB2MouseClickedEvent extends AB2MouseEvent {

    public AB2MouseClickedEvent(MouseEvent e) {
        this.mouseEvent=e;
    }

}
