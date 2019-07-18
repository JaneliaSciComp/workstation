package org.janelia.workstation.common.nb_action;

import java.awt.event.ActionEvent;

import org.janelia.workstation.common.gui.dialogs.RunAsUserDialog;
import org.janelia.workstation.core.api.AccessManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.SystemAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for running as another user. This is separate from RunAsMenuActionBuilder because it allows us to
 * add a hotkey using the NetBeans action registration framework.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "File",
        id = "RunAsMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_RunAsMenuAction",
        lazy = false
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "A-r")
})
@Messages("CTL_RunAsMenuAction=Run As")
public final class RunAsMenuAction extends SystemAction {

    private final static Logger log = LoggerFactory.getLogger(RunAsMenuAction.class);

    @Override
    public String getName() {
        return "Run As";
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (AccessManager.getAccessManager().isAdmin()) {
            RunAsUserDialog dialog = new RunAsUserDialog();
            dialog.showDialog();
        }
        else {
            log.warn("Cannot show run as user dialog to non-admin user");
        }
    }
}
