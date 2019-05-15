package org.janelia.workstation.common.nb_action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import org.janelia.workstation.common.gui.dialogs.ConnectDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "File",
        id = "ConnectMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_ConnectMenuAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/File", position = 1440, separatorBefore = 1425),
    @ActionReference(path = "Shortcuts", name = "A-c")
})
@Messages("CTL_ConnectMenuAction=Connect")
public final class ConnectMenuAction extends AbstractAction {
    
    @Override
    public void actionPerformed(ActionEvent e) {
        SwingUtilities.invokeLater(() -> {
            ConnectDialog dialog = new ConnectDialog();
            dialog.showDialog();
        });
    }
}
