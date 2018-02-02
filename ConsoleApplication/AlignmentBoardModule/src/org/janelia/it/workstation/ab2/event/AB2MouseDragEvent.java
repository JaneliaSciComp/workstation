package org.janelia.it.workstation.ab2.event;

import java.awt.event.MouseEvent;

public class AB2MouseDragEvent extends AB2MouseEvent {

    public AB2MouseDragEvent(MouseEvent e) {
        this.mouseEvent=e;
    }

}
