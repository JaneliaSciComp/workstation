package org.janelia.it.workstation.ab2.event;

import java.awt.event.MouseEvent;
import java.util.List;

import org.janelia.it.workstation.ab2.gl.GLSelectable;

public class AB2MouseDropEvent extends AB2MouseEvent {
    private List<GLSelectable> sourceObjects;

    public AB2MouseDropEvent(MouseEvent e, List<GLSelectable> sourceObjects) {
        this.mouseEvent=e;
        this.sourceObjects=sourceObjects;
    }

    public List<GLSelectable> getSourceObjects() { return sourceObjects; }
}
