package org.janelia.it.workstation.gui.browser.nb_action;

import org.janelia.it.workstation.gui.browser.api.StateMgr;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.CallableSystemAction;

@ActionID(
        category = "View",
        id = "org.janelia.it.workstation.gui.browser.nb_action.NavigateBack"
)
@ActionRegistration(
        displayName = "#CTL_NavigateBack"
)
@ActionReferences({
    @ActionReference(path = "Menu/View", position = -100, separatorBefore = -150),
    @ActionReference(path = "Toolbars/Navigation", position = 100),
    @ActionReference(path = "Shortcuts", name = "M-LEFT")
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
        StateMgr.getStateMgr().getNavigationHistory().goBack();
    }
}
