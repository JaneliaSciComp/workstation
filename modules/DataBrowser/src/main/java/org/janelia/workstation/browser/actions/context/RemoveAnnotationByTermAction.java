package org.janelia.workstation.browser.actions.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.ProgressMonitor;

import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.Category;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.model.domain.ontology.OntologyTermReference;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.events.selection.GlobalDomainObjectSelectionModel;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        @ActionReference(path = "Menu/actions/Ontology", position = 660)
})
@NbBundle.Messages("CTL_RemoveAnnotationByTermAction=Remove From Selected Objects")
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

        Collection<OntologyTerm> ontologyTerms = new ArrayList<>(this.selected);

        ActivityLogHelper.logUserAction("RemoveAnnotationByTermAction.performAction");

        final List<Reference> selectedIds = GlobalDomainObjectSelectionModel.getInstance().getSelectedIds();

        if (selectedIds.isEmpty()) {
            // Cannot annotate nothing
            log.warn("RemoveAnnotationByTermAction called without any objects being selected");
            return;
        }
        
        SimpleWorker worker = new SimpleWorker() {

            final DomainModel model = DomainMgr.getDomainMgr().getModel();
            
            @Override
            protected void doStuff() throws Exception {

                int i = 0;
                List<Annotation> annotations = model.getAnnotations(selectedIds);
                for (Annotation annotation : annotations) {
                    
                    OntologyTermReference keyTerm = annotation.getKeyTerm();
                    for(OntologyTerm ontologyTerm : ontologyTerms) {
                        if (keyTerm.getOntologyId().equals(ontologyTerm.getOntology().getId())
                                && keyTerm.getOntologyTermId().equals(ontologyTerm.getId())) {
                            log.info("Removing matching annotation: {}", annotation);
                            model.remove(annotation);
                            break;
                        }
                    }
                    
                    setProgress(i++, annotations.size());
                }
                        
            }

            @Override
            protected void hadSuccess() {
                // No need to do anything
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
            }
        };
        

        worker.setProgressMonitor(new ProgressMonitor(FrameworkAccess.getMainFrame(), "Deleting Annotations", "", 0, 100));
        worker.execute();
    }
}
