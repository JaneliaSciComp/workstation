package org.janelia.workstation.lm.actions;

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
        id = "org.janelia.workstation.lm.actions.LaunchFijiAction"
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
        return "images/fiji_16.png";
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
            ToolMgr.runToolSafely(ToolMgr.TOOL_FIJI);
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }
}
