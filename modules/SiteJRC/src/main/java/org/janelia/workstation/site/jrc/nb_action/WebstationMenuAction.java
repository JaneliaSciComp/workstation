package org.janelia.workstation.site.jrc.nb_action;

import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.util.Utils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

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
        Utils.openUrlInBrowser(WEBSTATION_URL);
    }
}
