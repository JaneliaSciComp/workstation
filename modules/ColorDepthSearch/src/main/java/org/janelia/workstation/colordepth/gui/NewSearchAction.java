package org.janelia.workstation.colordepth;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;

/**
 * Allows the user to create new color depth masks by uploading a PNG file.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "File",
        id = "org.janelia.workstation.colordepth.NewSearchAction"
)
@ActionRegistration(
        displayName = "Color Depth Search"
)
@ActionReferences({
        @ActionReference(path = "Menu/File/New", position = 20)
})
public final class NewSearchAction extends CallableSystemAction {

    @Override
    public String getName() {
        return "Color Depth Search";
    }

    @Override
    protected String iconResource() {
        return "images/drive_magnify.png";
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
        NewSearchActionListener actionListener = new NewSearchActionListener();
        actionListener.actionPerformed(null);
    }
}
