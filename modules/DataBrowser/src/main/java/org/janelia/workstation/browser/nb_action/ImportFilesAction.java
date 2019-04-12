package org.janelia.workstation.browser.nb_action;

import org.janelia.workstation.browser.gui.dialogs.ImportImageFilesDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.CallableSystemAction;

@ActionID(
        category = "File",
        id = "org.janelia.workstation.browser.nb_action.ImportAction"
)
@ActionRegistration(
        displayName = "#CTL_ImportFilesAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/File/Upload", position = 1),
    @ActionReference(path = "Shortcuts", name = "A-I")
})
@Messages("CTL_ImportFilesAction=Files")
public final class ImportFilesAction extends CallableSystemAction {

    @Override
    public String getName() {
        return "Image Files";
    }

    @Override
    protected String iconResource() {
        return "images/image.png";
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }
    
    @Override
    public void performAction() {
        ImportImageFilesDialog importDialog = new ImportImageFilesDialog();
        importDialog.showDialog();
    }
}
