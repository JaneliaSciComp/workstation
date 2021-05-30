package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.ontology.Category;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Removes annotations with a given key term from all the selected objects.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "actions",
        id = "RemoveAnnotationByTermAction"
)
@ActionRegistration(
        displayName = "#CTL_RemoveAnnotationByTermAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Ontology", position = 660)
})
@NbBundle.Messages("CTL_RemoveAnnotationByTermAction=Remove Term From Selected Objects")
public class RemoveAnnotationByTermAction extends BaseContextualNodeAction {

    private final static Logger log = LoggerFactory.getLogger(RemoveAnnotationByTermAction.class);

    private Collection<OntologyTerm> selected = new ArrayList<>();

    @Override
    protected void processContext() {
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
        else {
            setEnabledAndVisible(false);
        }
    }
    
    @Override
    public void performAction() {
        ActivityLogHelper.logUserAction("RemoveAnnotationByTermAction.performAction");
        Collection<OntologyTerm> ontologyTerms = new ArrayList<>(this.selected);
        RemoveAnnotationByTermActionListener action = new RemoveAnnotationByTermActionListener(ontologyTerms);
        action.actionPerformed(null);
    }
}
