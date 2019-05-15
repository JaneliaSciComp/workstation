package org.janelia.workstation.common.nb_action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.util.Utils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Help",
        id = "WebsiteMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_WebsiteMenuAction",
        lazy = true
)
@ActionReference(path = "Menu/Help", position = 130)
@Messages("CTL_WebsiteMenuAction=Workstation Website")
public final class WebsiteMenuAction extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent e) {
        ActivityLogHelper.logUserAction("WebsiteMenuAction.actionPerformed");
        String workstationUrl = ConsoleProperties.getInstance().getProperty("workstation.url");
        if (workstationUrl==null) {
            JOptionPane.showMessageDialog(
                    FrameworkAccess.getMainFrame(),
                    "Workstation website address was not found.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE,
                    null
            );
        }
        else {
            Utils.openUrlInBrowser(workstationUrl);
        }
    }
}
