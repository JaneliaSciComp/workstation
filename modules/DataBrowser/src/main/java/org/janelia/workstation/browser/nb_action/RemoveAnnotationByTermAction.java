package org.janelia.workstation.browser.nb_action;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ProgressMonitor;

import org.janelia.workstation.integration.FrameworkImplProvider;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.events.selection.GlobalDomainObjectSelectionModel;
import org.janelia.workstation.browser.nodes.OntologyTermNode;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.model.domain.ontology.OntologyTermReference;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Removes annotations with a given key term from all the selected objects.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RemoveAnnotationByTermAction extends NodeAction {

    private final static Logger log = LoggerFactory.getLogger(RemoveAnnotationByTermAction.class);
    
    private final static RemoveAnnotationByTermAction singleton = new RemoveAnnotationByTermAction();
    public static RemoveAnnotationByTermAction get() {
        return singleton;
    }
    
    private final List<OntologyTermNode> selected = new ArrayList<>();
    
    private RemoveAnnotationByTermAction() {
    }
    
    @Override
    public String getName() {
        return "Remove From Selected Objects";
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("");
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }
    
    @Override
    protected boolean enable(Node[] activatedNodes) {
        selected.clear();
        for(Node node : activatedNodes) {
            if (node instanceof OntologyTermNode) {
                selected.add((OntologyTermNode)node);
            }
        }
        // Enable state if at least one ontology term is selected
        return selected.size()>0;
    }
    
    @Override
    protected void performAction(Node[] activatedNodes) {
        if (!enable(activatedNodes)) return;
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
                    INNER: for(OntologyTermNode node : selected) {
                        OntologyTerm ontologyTerm = node.getOntologyTerm();
                        if (keyTerm.getOntologyId().equals(ontologyTerm.getOntology().getId())
                                && keyTerm.getOntologyTermId().equals(ontologyTerm.getId())) {
                            log.info("Removing matching annotation: {}", annotation);
                            model.remove(annotation);
                            break INNER;
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
                FrameworkImplProvider.handleException(error);
            }
        };
        

        worker.setProgressMonitor(new ProgressMonitor(FrameworkImplProvider.getMainFrame(), "Deleting Annotations", "", 0, 100));
        worker.execute();
    }
}
