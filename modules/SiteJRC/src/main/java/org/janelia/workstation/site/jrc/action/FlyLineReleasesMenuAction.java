package org.janelia.workstation.site.jrc.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.site.jrc.gui.dialogs.LineReleaseListDialog;

/**
 * This is deprecated and no longer shows up in the Services menu. At some point we can probably delete it, but I'm
 * keeping it for now in case we need to go back to some of the functionality quickly when 8.x is released.
 */
//@ActionID(
//        category = "Services",
//        id = "FlyLineReleasesMenuAction"
//)
//@ActionRegistration(
//        displayName = "#CTL_FlyLineReleasesMenuAction"
//)
//@ActionReference(path = "Menu/Services", position = 200)
//@Messages("CTL_FlyLineReleasesMenuAction=Fly Line Releases")
@Deprecated
public final class FlyLineReleasesMenuAction extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent e) {
        ActivityLogHelper.logUserAction("FlyLineReleasesMenuAction.actionPerformed");
        LineReleaseListDialog dialog = new LineReleaseListDialog();
        dialog.showDialog();
    }
}
