package org.janelia.workstation.controller.action;

import org.janelia.workstation.controller.dialog.NewTiledMicroscopeSampleDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.CallableSystemAction;

@ActionID(
        category = "File",
        id = "NewTiledMicroscopeSampleAction"
)
@ActionRegistration(
        displayName = "#CTL_NewTiledMicroscopeSampleAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/File/New", position = 1400)
})
@Messages("CTL_NewTiledMicroscopeSampleAction=Tiled Microscope Sample")
public final class NewTiledMicroscopeSampleAction extends CallableSystemAction {

    public NewTiledMicroscopeSampleAction() {
    }

    @Override
    public String getName() {
        return "Tiled Microscope Sample";
    }

    @Override
    protected String iconResource() {
        return "images/beaker.png";
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
        NewTiledMicroscopeSampleDialog dialog = new NewTiledMicroscopeSampleDialog();
        dialog.showForNewSample();
    }

}
