package org.janelia.it.workstation.ab2.event;

import java.awt.event.MouseEvent;

public class AB2MouseEnteredEvent extends AB2MouseEvent {

    public AB2MouseEnteredEvent(MouseEvent e) {
        this.mouseEvent=e;
    }

}

