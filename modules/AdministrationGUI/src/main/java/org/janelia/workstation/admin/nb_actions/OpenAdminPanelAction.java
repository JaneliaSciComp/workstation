package org.janelia.workstation.admin.nb_actions;
import java.awt.event.ActionEvent;

import org.janelia.workstation.admin.AdministrationTopComponent;
import org.janelia.workstation.core.api.AccessManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.SystemAction;
import org.slf4j.LoggerFactory;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

@ActionID(
        category = "Window",
        id = "org.janelia.workstation.admin.OpenAdminTopComponentAction"
)
@ActionRegistration(
        displayName = "#CTL_AdministrationTopComponentAction",
        lazy = false
)
@Messages("CTL_AdministrationTopComponentAction=Administration Tool")
public final class OpenAdminPanelAction extends SystemAction {

    @Override
    public String getName() {
        return "Administration Tool";
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (AccessManager.getAccessManager().isAdmin()) {
            TopComponent win = WindowManager.getDefault()
                    .findTopComponent(AdministrationTopComponent.PREFERRED_ID);
            if (win != null) {
                win.open();
                win.requestActive();
            }
        } else {
            // either silently disable, or tell the user
            LoggerFactory.getLogger(getClass())
                    .warn("Non-admin attempted to open Administration Tool");
        }
    }
}

