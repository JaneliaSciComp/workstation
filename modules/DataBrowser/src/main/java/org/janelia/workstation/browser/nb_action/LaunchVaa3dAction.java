package org.janelia.workstation.browser.nb_action;

import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.browser.tools.ToolMgr;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.CallableSystemAction;

@ActionID(
        category = "Tools",
        id = "org.janelia.workstation.browser.nb_action.LaunchVaa3dAction"
)
@ActionRegistration(
        displayName = "#CTL_LaunchVaa3dAction"
)
@ActionReferences({
    @ActionReference(path = "Toolbars/Tools", position = 101),
})
@Messages("CTL_LaunchVaa3dAction=Launch Vaa3d")
public final class LaunchVaa3dAction extends CallableSystemAction {

    @Override
    public String getName() {
        return "Launch Vaa3d";
    }

    @Override
    protected String iconResource() {
        return "images/v3d_16x16x32.png";
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
        try {
            ToolMgr.runToolSafely(ToolMgr.TOOL_VAA3D);
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }
}
