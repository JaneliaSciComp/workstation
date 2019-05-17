package org.janelia.workstation.site.jrc.nb_action;

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
        id = "WebstationMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_WebstationMenuAction",
        lazy = true
)
@ActionReference(path = "Menu/Help", position = 131)
@Messages("CTL_WebstationMenuAction=Webstation")
public final class WebstationMenuAction extends AbstractAction {
    
    @Override
    public void actionPerformed(ActionEvent e) {
        ActivityLogHelper.logUserAction("WebstationMenuAction.actionPerformed");
        String webstationUrl = ConsoleProperties.getInstance().getProperty("webstation.url");
        if (webstationUrl==null) {
            JOptionPane.showMessageDialog(
                    FrameworkAccess.getMainFrame(),
                    "Webstation address has not been configured.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE,
                    null
            );
        }
        else {
            Utils.openUrlInBrowser(webstationUrl);
        }
    }
}
