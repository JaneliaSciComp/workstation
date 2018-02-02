package org.janelia.it.workstation.ab2.event;

import java.awt.event.MouseEvent;

public class AB2MouseBeginDragEvent extends AB2MouseEvent {

    public AB2MouseBeginDragEvent(MouseEvent e) {
        this.mouseEvent=e;
    }

}
