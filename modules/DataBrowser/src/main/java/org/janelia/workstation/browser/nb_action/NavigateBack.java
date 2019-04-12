package org.janelia.workstation.browser.nb_action;

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
        id = "org.janelia.workstation.browser.nb_action.NavigateBack"
)
@ActionRegistration(
        displayName = "#CTL_NavigateBack"
)
@ActionReferences({
    @ActionReference(path = "Menu/View", position = 100, separatorBefore = 99),
    @ActionReference(path = "Toolbars/Navigation", position = 100),
    @ActionReference(path = "Shortcuts", name = "D-LEFT")
})
@Messages("CTL_NavigateBack=Back")
public final class NavigateBack extends CallableSystemAction {

    @Override
    public String getName() {
        return "Back";
    }

    @Override
    protected String iconResource() {
        return "images/arrow_back.gif";
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
        ActivityLogHelper.logUserAction("NavigateBack.performAction");
        DataBrowserMgr.getDataBrowserMgr().getNavigationHistory().goBack();
    }
}
