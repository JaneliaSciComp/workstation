package org.janelia.it.workstation.browser.nb_action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.gui.support.MailDialogueBox;
import org.janelia.it.workstation.browser.gui.support.WindowLocator;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Help",
        id = "ReportABugMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_ReportABugMenuAction",
        lazy = true
)
@ActionReference(path = "Menu/Help", position = 110)
@Messages("CTL_ReportABugMenuAction=Report A Bug")
public final class ReportABugMenuAction extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent e) {
        JFrame parentFrame = WindowLocator.getMainFrame();
        String email = (String) ConsoleApp.getConsoleApp().getModelProperty(AccessManager.USER_EMAIL);
        MailDialogueBox popup = new MailDialogueBox(parentFrame, email, 
                "[JW] Bug Report",
                "",
                "",
                "Create A Ticket",
                "Request Description");
        popup.showPopupThenSendEmail();
    }
}
