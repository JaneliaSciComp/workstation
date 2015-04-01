package org.janelia.it.workstation.gui.framework.console.nb_action;

import org.janelia.it.workstation.gui.framework.pref_controller.PrefController;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@ActionID(
        category = "Edit/Preferences",
        id = "UserAccountSettingsAction"
)
@ActionRegistration(
        displayName = "#CTL_UserAccountSettingsAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/Edit/Preferences", position = 100),
    @ActionReference(path = "Shortcuts", name = "M-F4")
})
@Messages("CTL_UserAccountSettingsAction=User Account Settings...")
public final class UserAccountSettingsAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        new EditingActionDelegate()
                .establishPrefController(PrefController.USER_ACCOUNT_EDITOR);
    }
}
