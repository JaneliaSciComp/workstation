package org.janelia.it.workstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
    @ActionReference(path = "Menu/Edit/Preferences", position = 200)
})
@Messages("CTL_ViewerPreferencesAction=Preferences have been moved")
public final class ViewerPreferencesAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
    }
}
