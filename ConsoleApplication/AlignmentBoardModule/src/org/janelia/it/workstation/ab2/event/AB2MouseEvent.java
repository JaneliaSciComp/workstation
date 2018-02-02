package org.janelia.it.workstation.ab2.event;

import java.awt.event.MouseEvent;

public abstract class AB2MouseEvent extends AB2Event {

    protected MouseEvent mouseEvent;

    public MouseEvent getMouseEvent() { return mouseEvent; }

    public void setMouseEvent(MouseEvent mouseEvent) { this.mouseEvent=mouseEvent; }

}
