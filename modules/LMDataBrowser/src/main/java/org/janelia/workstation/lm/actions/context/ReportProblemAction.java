package org.janelia.workstation.lm.actions.context;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.util.ErrorReportDialogueBox;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;


/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "actions",
        id = "ReportProblemAction"
)
@ActionRegistration(
        displayName = "#CTL_ReportProblemAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Sample", position = 500, separatorBefore = 499)
})
@NbBundle.Messages("CTL_ReportProblemAction=Report A Problem With This Data")
public class ReportProblemAction extends BaseContextualNodeAction {

    private static final Logger log = LoggerFactory.getLogger(ReportProblemAction.class);

    private Sample selectedObject;

    @Override
    protected void processContext() {
        if (getNodeContext().isSingleObjectOfType(Sample.class)) {
            this.selectedObject = getNodeContext().getSingleObjectOfType(Sample.class);
            setEnabledAndVisible(true);
        } else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    public void performAction() {

        DomainObject domainObject = selectedObject;
        ActivityLogHelper.logUserAction("DomainObjectContentMenu.reportAProblemWithThisData", domainObject);

        try {
            reportData(domainObject);

            JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                    "Successfully reported problem with " + domainObject.getName(),
                    "Data Problem Reported", JOptionPane.PLAIN_MESSAGE);
        } catch (Exception ex) {
            FrameworkAccess.handleException(ex);
        }
    }

    private void reportData(DomainObject domainObject) {

        String subject = "Reported Data: " + domainObject.getName();

        // this dialog box will not be displayed; we do not call errorReportDialogueBox.showPopup()
        ErrorReportDialogueBox errorReportDialogueBox = ErrorReportDialogueBox.newDialog(FrameworkAccess.getMainFrame())
                .withTitle("not displayed")
                .withPromptText("not displayed")
                .withSubject(subject);

        errorReportDialogueBox.append(createEntityReport(domainObject));
        errorReportDialogueBox.sendReport();
    }

    private String createEntityReport(DomainObject domainObject) {
        StringBuilder sBuf = new StringBuilder();

        String user = AccessManager.getSubjectKey();
        sBuf.append("Reporting user: ").append(user).append("\n");

        sBuf.append("GUID: ").append(domainObject.getId().toString()).append("\n");
        sBuf.append("Type: ").append(domainObject.getType()).append("\n");
        sBuf.append("Owner: ").append(domainObject.getOwnerKey()).append("\n");
        sBuf.append("Name: ").append(domainObject.getName()).append("\n");
        return sBuf.toString();
    }
}
