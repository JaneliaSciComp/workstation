package org.janelia.it.workstation.gui.browser.nb_action;

import java.awt.Component;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
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
        id = "org.janelia.it.workstation.gui.browser.nb_action.NewFolderAction"
)
@ActionRegistration(
        displayName = "#CTL_NewFolderAction"
)
@ActionReferences({
        @ActionReference(path = "Menu/File/New", position = 1),
        @ActionReference(path = "Toolbars/Navigation", position = 1)
})
@Messages("CTL_NewFolderAction=Folder")
public final class NewFolderAction extends CallableSystemAction {

    protected final Component mainFrame = SessionMgr.getMainFrame();

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
