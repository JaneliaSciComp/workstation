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
        id = "org.janelia.it.workstation.gui.browser.nb_action.LaunchFijiAction"
)
@ActionRegistration(
        displayName = "#CTL_LaunchFijiAction"
)
@ActionReferences({
    @ActionReference(path = "Toolbars/Tools", position = 100),
})
@Messages("CTL_LaunchFijiAction=Launch Fiji")
public final class LaunchFijiAction extends CallableSystemAction {

    @Override
    public String getName() {
        return "Launch Fiji";
    }

    @Override
    protected String iconResource() {
        return "images/brain.png";
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
            ToolMgr.runTool(ToolMgr.TOOL_FIJI);
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }
}
