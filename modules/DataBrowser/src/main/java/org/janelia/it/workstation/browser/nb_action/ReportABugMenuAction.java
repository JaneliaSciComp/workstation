package org.janelia.it.workstation.browser.nb_action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import org.janelia.it.workstation.browser.util.MailDialogueBox;
import org.janelia.it.workstation.browser.gui.support.WindowLocator;
import org.janelia.it.workstation.browser.logging.NBExceptionHandler;
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

        MailDialogueBox popup = MailDialogueBox.newDialog(parentFrame, NBExceptionHandler.REPORT_EMAIL)
                .withTitle("Create A Ticket")
                .withPromptText("Problem Description:")
                .withEmailSubject("Bug Report")
                .appendStandardPrefix()
                .append("\n\nMessage:\n");
        
        String desc = popup.showPopup();
        if (desc!=null) {
            popup.appendLine(desc);
            popup.sendEmail();
        }
        
    }
}
