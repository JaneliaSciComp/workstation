package org.janelia.workstation.browser.actions;

import org.janelia.workstation.browser.api.state.DataBrowserMgr;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.CallableSystemAction;

@ActionID(
        category = "View",
        id = "org.janelia.workstation.browser.actions.NavigateForward"
)
@ActionRegistration(
        displayName = "#CTL_NavigateForward"
)
@ActionReferences({
    @ActionReference(path = "Menu/View", position = 101, separatorAfter = 102),
    @ActionReference(path = "Toolbars/Navigation", position = 101),
    @ActionReference(path = "Shortcuts", name = "D-RIGHT")
})
@Messages("CTL_NavigateForward=Forward")
public final class NavigateForward extends CallableSystemAction {

    @Override
    public String getName() {
        return "Forward";
    }

    @Override
    protected String iconResource() {
        return "images/arrow_forward.gif";
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
        ActivityLogHelper.logUserAction("NavigateForward.performAction");
        DataBrowserMgr.getDataBrowserMgr().getNavigationHistory().goForward();
    }
}
