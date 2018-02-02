package org.janelia.it.workstation.ab2.event;

import java.awt.event.ActionEvent;

public class AB2AwtActionEvent extends AB2Event {
    private ActionEvent actionEvent;

    public AB2AwtActionEvent(ActionEvent e) {
        this.actionEvent=e;
    }

    public ActionEvent getActionEvent() {
        return actionEvent;
    }
}
