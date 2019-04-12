package org.janelia.it.workstation.browser.nb_action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
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

    private static final String WORKSTATION_URL = ConsoleProperties.getInstance().getProperty("workstation.url");
    
    @Override
    public void actionPerformed(ActionEvent e) {
        ActivityLogHelper.logUserAction("WebsiteMenuAction.actionPerformed");
        Utils.openUrlInBrowser(WORKSTATION_URL);
    }
}
