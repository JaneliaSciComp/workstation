package org.janelia.workstation.browser.actions.context;

import java.util.ArrayList;
import java.util.Collection;

import org.janelia.model.domain.ontology.Category;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.workstation.browser.gui.options.BrowserOptions;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.util.actions.SystemAction;

/**
 * Create an annotation by applying the current ontology term to the 
 * domain objects selected in the active browser. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "Actions",
        id = "ApplyAnnotationAction"
)
@ActionRegistration(
        displayName = "#CTL_ApplyAnnotationAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Ontology", position = 650, separatorBefore = 649)
})
@NbBundle.Messages("CTL_ApplyAnnotationAction=Apply To Selected Objects")
public class ApplyAnnotationAction extends BaseContextualNodeAction {

    public static ApplyAnnotationAction get() {
        return SystemAction.get(ApplyAnnotationAction.class);
    }

    private boolean isDuplicateAnnotationAllowed;
    private Collection<OntologyTerm> selected = new ArrayList<>();

    @Override
    protected void processContext() {
        this.isDuplicateAnnotationAllowed = BrowserOptions.getInstance().isDuplicateAnnotationAllowed();
        selected.clear();
        if (getNodeContext().isOnlyObjectsOfType(OntologyTerm.class)) {
            for (OntologyTerm term : getNodeContext().getOnlyObjectsOfType(OntologyTerm.class)) {
                if (term instanceof Category || term instanceof Ontology || term instanceof org.janelia.model.domain.ontology.Enum) {
                    // Can't apply these as an annotation
                    continue;
                }
                selected.add(term);
            }

            setEnabledAndVisible(selected.size()>0);
        }
        else if (getViewerContext()!=null) {
            setEnabledAndVisible(true);
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    public void setDuplicateAnnotationAllowed(boolean duplicateAnnotationAllowed) {
        this.isDuplicateAnnotationAllowed = duplicateAnnotationAllowed;
    }

    @Override
    public void performAction() {
        Collection<OntologyTerm> ontologyTerms = new ArrayList<>(this.selected);
        ApplyAnnotationActionListener action = new ApplyAnnotationActionListener(ontologyTerms);
        action.actionPerformed(null);
    }
}
