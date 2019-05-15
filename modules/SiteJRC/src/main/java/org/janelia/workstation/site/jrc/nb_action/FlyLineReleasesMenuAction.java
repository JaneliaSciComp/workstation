package org.janelia.workstation.site.jrc.nb_action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.site.jrc.gui.dialogs.LineReleaseListDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Services",
        id = "FlyLineReleasesMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_FlyLineReleasesMenuAction",
        lazy = false
)
@ActionReference(path = "Menu/Services", position = 200)
@Messages("CTL_FlyLineReleasesMenuAction=Fly Line Releases")
public final class FlyLineReleasesMenuAction extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent e) {
        ActivityLogHelper.logUserAction("FlyLineReleasesMenuAction.actionPerformed");
        LineReleaseListDialog dialog = new LineReleaseListDialog();
        dialog.showDialog();
    }
}
