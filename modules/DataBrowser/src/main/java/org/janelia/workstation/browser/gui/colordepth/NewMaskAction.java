package org.janelia.workstation.browser.gui.colordepth;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.CallableSystemAction;

/**
 * Allows the user to create new color depth masks by uploading a PNG file.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "File",
        id = "org.janelia.workstation.browser.nb_action.NewMaskAction"
)
@ActionRegistration(
        displayName = "#CTL_NewColorDepthMaskAction"
)
@ActionReferences({
        @ActionReference(path = "Menu/File/Upload", position = 10)
})
@Messages("CTL_NewColorDepthMaskAction=Color Depth Mask")
public final class NewMaskAction extends CallableSystemAction {

    @Override
    public String getName() {
        return "Color Depth Mask";
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
        NewMaskActionListener actionListener = new NewMaskActionListener();
        actionListener.actionPerformed(null);
    }
}
