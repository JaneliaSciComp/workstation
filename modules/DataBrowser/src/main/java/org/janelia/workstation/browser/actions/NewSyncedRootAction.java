package org.janelia.workstation.browser.actions;

import org.janelia.workstation.browser.gui.dialogs.SyncedRootDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;
import org.openide.util.actions.SystemAction;

@ActionID(
        category = "File",
        id = "NewSyncedRootAction"
)
@ActionRegistration(
        displayName = "#CTL_NewSyncedRootAction"
)
@ActionReferences({
        @ActionReference(path = "Menu/File/New", position = 15)
})
@NbBundle.Messages("CTL_NewSyncedRootAction=Synchronized Folder")
public class NewSyncedRootAction extends CallableSystemAction {

    public static NewSyncedRootAction get() {
        return SystemAction.get(NewSyncedRootAction.class);
    }

    @Override
    public String getName() {
        return "Synchronized Folder";
    }

    @Override
    protected String iconResource() {
        return "images/folder_database.png";
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
        SyncedRootDialog dialog = new SyncedRootDialog();
        dialog.showDialog();
    }

}
