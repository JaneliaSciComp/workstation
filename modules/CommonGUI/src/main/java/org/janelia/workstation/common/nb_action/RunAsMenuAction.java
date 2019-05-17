package org.janelia.workstation.common.nb_action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.model.domain.enums.SubjectRole;
import org.janelia.workstation.common.gui.dialogs.RunAsUserDialog;
import org.janelia.workstation.core.api.AccessManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "File",
        id = "RunAsMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_RunAsMenuAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/File", position = 1451),
    @ActionReference(path = "Shortcuts", name = "A-r")
})
@Messages("CTL_RunAsMenuAction=Run As")
public final class RunAsMenuAction extends AbstractAction {
    
    @Override
    public void actionPerformed(ActionEvent e) {
        RunAsUserDialog dialog = new RunAsUserDialog();
        dialog.showDialog();
    }

    @Override
    public boolean isEnabled() {
        return AccessManager.authenticatedSubjectIsInGroup(SubjectRole.Admin);
    }
}
