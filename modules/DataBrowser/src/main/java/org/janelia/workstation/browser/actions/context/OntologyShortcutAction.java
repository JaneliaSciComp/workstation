package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.workstation.browser.gui.components.OntologyExplorerTopComponent;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 * Builds an action to assign a shortcut to the selected ontology term.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "actions",
        id = "OntologyShortcutAction"
)
@ActionRegistration(
        displayName = "#CTL_OntologyShortcutAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/actions/Ontology", position = 620, separatorBefore = 599)
})
@NbBundle.Messages("CTL_OntologyShortcutAction=Assign Shortcut...")
public class OntologyShortcutAction extends BaseContextualNodeAction {

    private OntologyTerm selectedTerm;

    @Override
    protected void processContext() {
        if (getNodeContext().isSingleObjectOfType(OntologyTerm.class)) {
            selectedTerm = getNodeContext().getSingleObjectOfType(OntologyTerm.class);
            setEnabledAndVisible(true);
        }
        else {
            selectedTerm = null;
            setEnabledAndVisible(false);
        }
    }

    @Override
    public void performAction() {
        OntologyExplorerTopComponent.getInstance().showKeyBindDialog(selectedTerm);
    }
}
