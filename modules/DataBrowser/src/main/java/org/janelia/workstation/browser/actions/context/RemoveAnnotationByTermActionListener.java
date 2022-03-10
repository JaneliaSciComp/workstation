package org.janelia.workstation.browser.actions.context;

import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.model.domain.ontology.OntologyTermReference;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.events.selection.GlobalDomainObjectSelectionModel;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RemoveAnnotationByTermActionListener implements ActionListener {

    private final static Logger log = LoggerFactory.getLogger(RemoveAnnotationByTermActionListener.class);

    private Collection<OntologyTerm> ontologyTerms;

    public RemoveAnnotationByTermActionListener(Collection<OntologyTerm> ontologyTerms) {
        this.ontologyTerms = ontologyTerms;
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        final List<Reference> selectedIds = GlobalDomainObjectSelectionModel.getInstance().getSelectedIds();
        log.info("Removing annotations from {} objects", selectedIds.size());

        if (selectedIds.isEmpty()) {
            // Cannot annotate nothing
            log.warn("RemoveAnnotationByTermAction called without any objects being selected");
            return;
        }

        SimpleWorker worker = new SimpleWorker() {

            final DomainModel model = DomainMgr.getDomainMgr().getModel();

            @Override
            protected void doStuff() throws Exception {

                List<Annotation> toRemove = new ArrayList<>();
                List<Annotation> annotations = model.getAnnotations(selectedIds);
                for (Annotation annotation : annotations) {
                    if (ClientDomainUtils.hasWriteAccess(annotation)) {
                        OntologyTermReference keyTerm = annotation.getKeyTerm();
                        if (keyTerm!=null) {
                            for (OntologyTerm ontologyTerm : ontologyTerms) {
                                if (keyTerm.getOntologyId().equals(ontologyTerm.getOntology().getId())
                                        && keyTerm.getOntologyTermId().equals(ontologyTerm.getId())) {
                                    toRemove.add(annotation);
                                    break;
                                }
                            }
                        }
                    }
                }

                int i = 1;
                for(Annotation annotation : toRemove) {
                    log.info("Removing {} - {}", i, annotation);
                    model.removeAnnotation(annotation);
                    setProgress(i++, toRemove.size());
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
