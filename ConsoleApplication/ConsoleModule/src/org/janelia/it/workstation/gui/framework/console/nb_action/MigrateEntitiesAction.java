package org.janelia.it.workstation.gui.framework.console.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


import org.janelia.it.workstation.gui.dialogs.MigrateEntitiesDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Services",
        id = "MigrateEntitiesAction"
)
@ActionRegistration(
        displayName = "#CTL_MigrateEntitiesAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/Services", position = 1500),
    @ActionReference(path = "Shortcuts", name = "M-N")
})
@Messages("CTL_MigrateEntitiesAction=Migrate References and Annotations")
public final class MigrateEntitiesAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        new MigrateEntitiesDialog();
    }
}
