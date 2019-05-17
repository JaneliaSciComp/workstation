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
        id = "UserManualMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_UserGuideMenuAction",
        lazy = true
)
@ActionReference(path = "Menu/Help", position = 120)
@Messages("CTL_UserGuideMenuAction=User Manual")
public final class UserManualMenuAction extends AbstractAction {
    
    @Override
    public void actionPerformed(ActionEvent e) {
        ActivityLogHelper.logUserAction("UserManualMenuAction.actionPerformed");
        String manualUrl = ConsoleProperties.getInstance().getProperty("manual.url");
        if (manualUrl==null) {
            JOptionPane.showMessageDialog(
                    FrameworkAccess.getMainFrame(),
                    "User manual address has not been configured.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE,
                    null
            );
        }
        else {
            Utils.openUrlInBrowser(manualUrl);
        }
    }
}
