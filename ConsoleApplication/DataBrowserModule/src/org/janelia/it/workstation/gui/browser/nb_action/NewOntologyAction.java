package org.janelia.it.workstation.gui.browser.nb_action;

import java.awt.Component;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.CallableSystemAction;

/**
 * Create a new ontology owned by the current user.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "File",
        id = "NewOntologyAction"
)
@ActionRegistration(
        displayName = "#CTL_NewOntologyAction"
)
@ActionReference(path = "Menu/File/New", position = 4)
@Messages("CTL_NewOntologyAction=Ontology")
public class NewOntologyAction extends CallableSystemAction {

    protected final Component mainFrame = SessionMgr.getMainFrame();

    public NewOntologyAction() {
    }

    @Override
    public String getName() {
        return "Ontology";
    }

    @Override
    protected String iconResource() {
        return "images/page_add.png";
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
        NewOntologyActionListener actionListener = new NewOntologyActionListener();
        actionListener.actionPerformed(null);
    }
}
