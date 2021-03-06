package org.janelia.workstation.browser.actions;

import java.awt.Component;

import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.CallableSystemAction;

/**
 * Allows the user to create new folders, either in their default workspace, 
 * or underneath another existing tree node. Once the folder is created, it is
 * selected in the tree.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "File",
        id = "org.janelia.workstation.browser.actions.NewFolderAction"
)
@ActionRegistration(
        displayName = "#CTL_NewFolderAction"
)
@ActionReferences({
        @ActionReference(path = "Menu/File/New", position = 2),
        @ActionReference(path = "Toolbars/Navigation", position = 2)
})
@Messages("CTL_NewFolderAction=Folder")
public final class NewFolderAction extends CallableSystemAction {

    protected final Component mainFrame = FrameworkAccess.getMainFrame();

    public NewFolderAction() {
    }

    @Override
    public String getName() {
        return "Folder";
    }

    @Override
    protected String iconResource() {
        return "images/folder_add.png";
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
        NewFolderActionListener actionListener = new NewFolderActionListener();
        actionListener.actionPerformed(null);
    }
}
