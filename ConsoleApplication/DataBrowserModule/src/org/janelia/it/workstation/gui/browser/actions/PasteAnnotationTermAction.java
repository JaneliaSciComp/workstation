package org.janelia.it.workstation.gui.browser.actions;

import java.util.List;

import javax.swing.ProgressMonitor;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.api.StateMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

/**
 * Paste the Annotation in the clipboard onto one or more items.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PasteAnnotationTermAction implements NamedAction {

    private final List<DomainObject> selectedObjects;

    public PasteAnnotationTermAction(List<DomainObject> selectedObjects) {
        this.selectedObjects = selectedObjects;
    }

    @Override
    public String getName() {
        return "Paste Annotation";
    }

    @Override
    public void doAction() {
    
        final DomainModel model = DomainMgr.getDomainMgr().getModel();

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {

                int i = 1;
                for (DomainObject domainObject : selectedObjects) {
                    Annotation baseAnnotation = StateMgr.getStateMgr().getCurrentSelectedOntologyAnnotation();
                    Annotation annotation = new Annotation(baseAnnotation);
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
                SessionMgr.getSessionMgr().handleException(error);
            }
        };

        worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getMainFrame(), "Pasting Annotations", "", 0, 100));
        worker.execute();
    }
}
