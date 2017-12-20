package org.janelia.it.workstation.ab2.event;

import java.awt.event.MouseEvent;

public class AB2MouseDraggedEvent extends AB2MouseEvent {

    public AB2MouseDraggedEvent(MouseEvent e) {
        this.mouseEvent=e;
    }

}
