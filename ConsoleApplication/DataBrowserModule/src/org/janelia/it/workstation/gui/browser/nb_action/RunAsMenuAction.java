package org.janelia.it.workstation.gui.browser.nb_action;

import org.janelia.it.jacs.model.domain.enums.SubjectRole;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import org.janelia.it.workstation.gui.dialogs.RunAsUserDialog;
import org.openide.awt.ActionReferences;

@ActionID(
        category = "File",
        id = "RunAsMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_RunAsMenuAction",
        lazy = false
)
@ActionReferences({
    @ActionReference(path = "Menu/File", position = 1451),
    @ActionReference(path = "Shortcuts", name = "A-r")
})
@Messages("CTL_RunAsMenuAction=Run As")
public final class RunAsMenuAction extends AbstractAction implements Presenter.Menu {
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (isAccessible()) {
            RunAsUserDialog dialog = new RunAsUserDialog();
            dialog.showDialog();
        }
    }

    @Override
    public JMenuItem getMenuPresenter() {
        if (isAccessible()) {
            JMenuItem menuItem = new JMenuItem("Run As");
            menuItem.addActionListener(this);
            return menuItem;
        }
        return null;
    }

    public static boolean isAccessible() {
        return AccessManager.authenticatedSubjectIsInGroup(SubjectRole.Admin);
    }
}
