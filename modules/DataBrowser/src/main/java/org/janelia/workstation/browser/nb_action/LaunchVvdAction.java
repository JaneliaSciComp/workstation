package org.janelia.workstation.browser.nb_action;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
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
        id = "org.janelia.workstation.browser.nb_action.LaunchVvdAction"
)
@ActionRegistration(
        displayName = "#CTL_LaunchVvdAction"
)
@ActionReferences({
    @ActionReference(path = "Toolbars/Tools", position = 102),
})
@Messages("CTL_LaunchVvdAction=Launch VVD")
public final class LaunchVvdAction extends CallableSystemAction {

    @Override
    public String getName() {
        return "Launch VVD";
    }

    @Override
    protected String iconResource() {
        return "images/vvd16.svg.png";
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
            ToolMgr.runToolSafely(ToolMgr.TOOL_VVD);
        }
        catch (Exception e) {
            FrameworkImplProvider.handleException(e);
        }
    }
}
