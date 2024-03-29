package org.janelia.workstation.browser.actions;

import org.janelia.model.domain.workspace.TreeNode;
import org.janelia.workstation.browser.gui.components.DomainListViewManager;
import org.janelia.workstation.browser.gui.components.DomainListViewTopComponent;
import org.janelia.workstation.browser.gui.components.ViewerUtils;
import org.janelia.workstation.browser.gui.dialogs.identifiers.IdentifiersWizardIterator;
import org.janelia.workstation.browser.gui.dialogs.identifiers.IdentifiersWizardState;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.CallableSystemAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.text.MessageFormat;

/**
 * Action which brings up the Import Identifiers wizard 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "Services",
        id = "org.janelia.workstation.browser.actions.ImportIdentifiersAction"
)
@ActionRegistration(
        displayName = "#CTL_ImportIdentifiersAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/Services", position = 20),
    @ActionReference(path = "Shortcuts", name = "A-U")
})
@Messages("CTL_ImportIdentifiersAction=Batch Search")
public final class ImportIdentifiersAction extends CallableSystemAction {

    private static final Logger log = LoggerFactory.getLogger(ImportIdentifiersAction.class);

    private static Long resultId = -1L;

    @Override
    public String getName() {
        return "Batch Search";
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

        // Hide the default wizard image, which does not look good on our dark background
        UIDefaults uiDefaults = UIManager.getDefaults();
        uiDefaults.put("nb.wizard.hideimage", Boolean.TRUE); 
        
        // Create wizard
        IdentifiersWizardIterator iterator = new IdentifiersWizardIterator();
        WizardDescriptor wiz = new WizardDescriptor(iterator);
        iterator.initialize(wiz); 

        // {0} will be replaced by WizardDescriptor.Panel.getComponent().getName()
        // {1} will be replaced by WizardDescriptor.Iterator.name()
        wiz.setTitleFormat(new MessageFormat("{0} ({1})"));
        wiz.setTitle(getName());

        // Install the state
        wiz.putProperty(IdentifiersWizardIterator.PROP_WIZARD_STATE, new IdentifiersWizardState());
        
        // Show the wizard
        if (DialogDisplayer.getDefault().notify(wiz) == WizardDescriptor.FINISH_OPTION) {

            IdentifiersWizardState endState = (IdentifiersWizardState) wiz.getProperty(IdentifiersWizardIterator.PROP_WIZARD_STATE);

            TreeNode node = new TreeNode();
            node.setId(resultId--); // decrement search result
            node.setName("Batch Search Results");
            node.setChildren(endState.getResults());

            DomainListViewTopComponent targetViewer = ViewerUtils.provisionViewer(DomainListViewManager.getInstance(), "editor");
            // If we are reacting to a selection event in another viewer, then this load is not user driven.
            targetViewer.loadDomainObject(node, true);
        }
    }

}
