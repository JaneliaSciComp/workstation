package org.janelia.it.workstation.gui.browser.nb_action;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.tool_manager.ToolMgr;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.CallableSystemAction;

@ActionID(
        category = "Tools",
        id = "org.janelia.it.workstation.gui.browser.nb_action.LaunchVaa3dAction"
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
            ToolMgr.runTool(ToolMgr.TOOL_VAA3D);
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }
}
