package org.janelia.workstation.common.nb_action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.workstation.core.logging.LoggingUtils;
import org.janelia.workstation.core.util.ErrorReportDialogueBox;
import org.janelia.workstation.integration.util.FrameworkAccess;
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

        String subject = LoggingUtils.getErrorReportSubject(false)+" -- Bug Report";

        ErrorReportDialogueBox popup = ErrorReportDialogueBox.newDialog(FrameworkAccess.getMainFrame())
                .withTitle("Create A Ticket")
                .withPromptText("Problem Description:")
                .withSubject(subject)
                .appendStandardPrefix()
                .append("\n\nMessage:\n");
        
        String desc = popup.showPopup();
        if (desc!=null) {
            popup.appendLine(desc);
            popup.sendReport();
        }
        
    }
}
