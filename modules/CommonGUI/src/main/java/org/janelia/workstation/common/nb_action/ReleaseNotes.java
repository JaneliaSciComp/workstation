package org.janelia.workstation.common.nb_action;

import org.janelia.workstation.common.gui.lifecycle.ShowingHook;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionID(
        category = "Help",
        id = "org.janelia.it.workstation.browser.nb_action.ReleaseNotes"
)
@ActionRegistration(
        displayName = "#CTL_ReleaseNotes"
)
@ActionReferences({
    @ActionReference(path = "Menu/Help", position = 101)
})
@Messages("CTL_ReleaseNotes=View Release Notes")
public final class ReleaseNotes extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent e) {
        ShowingHook.getReleaseNotesDialog().showCurrentReleaseNotes();
    }

}
