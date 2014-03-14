/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.FlyWorkstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.janelia.it.FlyWorkstation.gui.framework.pref_controller.PrefController;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Edit/Preferences",
        id = "org.janelia.it.FlyWorkstation.gui.framework.console.nb_action.SystemPreferencesAction"
)
@ActionRegistration(
        displayName = "#CTL_SystemPreferencesAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/Edit/Preferences", position = 100),
    @ActionReference(path = "Shortcuts", name = "C-S")
})
@Messages("CTL_SystemPreferencesAction=System...")
public final class SystemPreferencesAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        new EditingActionDelegate()
                .establishPrefController(PrefController.APPLICATION_EDITOR);
    }
}
