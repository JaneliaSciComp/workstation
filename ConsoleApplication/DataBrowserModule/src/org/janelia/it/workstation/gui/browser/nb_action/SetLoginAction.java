package org.janelia.it.workstation.gui.browser.nb_action;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.janelia.it.workstation.gui.browser.gui.dialogs.LoginDialog;

@ActionID(
        category = "File",
        id = "SetLoginAction"
)
@ActionRegistration(
        displayName = "#CTL_SetLoginAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/File", position = 1450, separatorBefore = 1425),
    @ActionReference(path = "Shortcuts", name = "A-o")
})
@Messages("CTL_SetLoginAction=Set Login")
public final class SetLoginAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        LoginDialog dialog = new LoginDialog();
        dialog.showDialog();
    }
}
