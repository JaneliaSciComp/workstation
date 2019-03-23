package org.janelia.it.workstation.browser.nb_action;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.CallableSystemAction;

/**
 * Yet another way to create a new search.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "Services",
        id = "org.janelia.it.workstation.browser.nb_action.SearchAction"
)
@ActionRegistration(
        displayName = "#CTL_SearchAction"
)
@ActionReferences({
        @ActionReference(path = "Menu/Services", position = 10)
})
@Messages("CTL_SearchAction=Search")
public final class SearchAction extends CallableSystemAction {

    public SearchAction() {
    }

    @Override
    public String getName() {
        return "Search";
    }

    @Override
    protected String iconResource() {
        return "images/search-white-icon.png";
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
        NewFilterActionListener actionListener = new NewFilterActionListener();
        actionListener.actionPerformed(null);
    }
}
