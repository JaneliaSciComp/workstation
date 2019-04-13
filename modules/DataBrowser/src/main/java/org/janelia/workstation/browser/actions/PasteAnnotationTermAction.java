package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ProgressMonitor;

import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.api.StateMgr;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;

/**
 * Paste the Annotation in the clipboard onto one or more items.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PasteAnnotationTermAction extends AbstractAction {

    private final List<DomainObject> selectedObjects;

    public PasteAnnotationTermAction(List<DomainObject> selectedObjects) {
        super("Paste Annotation");
        this.selectedObjects = selectedObjects;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        ActivityLogHelper.logUserAction("PasteAnnotationTermAction.doAction");

        final DomainModel model = DomainMgr.getDomainMgr().getModel();

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {

                int i = 1;
                for (DomainObject domainObject : selectedObjects) {
                    Annotation baseAnnotation = StateMgr.getStateMgr().getCurrentSelectedOntologyAnnotation();
                    Annotation annotation = new Annotation(baseAnnotation);
                    // We may be copying an annotation we don't own. Don't copy the ownership.
                    annotation.setOwnerKey(null);
                    annotation.setReaders(Collections.emptySet());
                    annotation.setWriters(Collections.emptySet());
                    annotation.setTarget(Reference.createFor(domainObject));
                    annotation = model.create(annotation);
                    setProgress(i++, selectedObjects.size());
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

        worker.setProgressMonitor(new ProgressMonitor(FrameworkAccess.getMainFrame(), "Pasting Annotations", "", 0, 100));
        worker.execute();
    }
}
