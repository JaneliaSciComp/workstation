/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.janelia.it.workstation.gui.framework.pref_controller.PrefController;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Edit/Preferences",
        id = "ViewerPreferencesAction"
)
@ActionRegistration(
        displayName = "#CTL_ViewerPreferencesAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/Edit/Preferences", position = 200),
    @ActionReference(path = "Shortcuts", name = "C-V")
})
@Messages("CTL_ViewerPreferencesAction=Viewer")
public final class ViewerPreferencesAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        new EditingActionDelegate()
                .establishPrefController(PrefController.VIEWER_EDITOR);
    }
}
