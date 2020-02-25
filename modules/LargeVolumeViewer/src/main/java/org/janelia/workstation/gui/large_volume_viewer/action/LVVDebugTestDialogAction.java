package org.janelia.workstation.gui.large_volume_viewer.action;

import org.janelia.workstation.gui.large_volume_viewer.ComponentUtil;
import org.janelia.workstation.gui.large_volume_viewer.dialogs.LVVDebugTestDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LVVDebugTestDialogAction extends AbstractAction {

    private static final Logger logger = LoggerFactory.getLogger(BulkChangeNeuronColorAction.class);

    public LVVDebugTestDialogAction() {
        putValue(NAME, "Open LVV debug/test dialog...");
        putValue(SHORT_DESCRIPTION, "Open a dialog with LVV/Horta test and debug tools");
    }

    public void actionPerformed(ActionEvent event) {
        LVVDebugTestDialog dialog = new LVVDebugTestDialog((Frame) SwingUtilities.windowForComponent(ComponentUtil.getLVVMainWindow()));
        dialog.setVisible(true);
    }
}
