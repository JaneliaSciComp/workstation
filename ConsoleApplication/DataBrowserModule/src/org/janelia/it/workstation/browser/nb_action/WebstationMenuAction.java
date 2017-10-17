package org.janelia.it.workstation.browser.nb_action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.janelia.it.workstation.browser.util.Utils;
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

    private static final String WEBSTATION_URL = ConsoleProperties.getInstance().getProperty("webstation.url"); 
    
    @Override
    public void actionPerformed(ActionEvent e) {
        ActivityLogHelper.logUserAction("WebstationMenuAction.actionPerformed");
        Utils.openUrlInBrowser(WEBSTATION_URL);
    }
}
