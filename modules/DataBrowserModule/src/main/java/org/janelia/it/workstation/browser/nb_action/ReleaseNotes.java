package org.janelia.it.workstation.browser.nb_action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

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
        ConsoleApp.getConsoleApp().getReleaseNotesDialog().showCurrentReleaseNotes();
    }

}
