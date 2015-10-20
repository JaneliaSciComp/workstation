package org.janelia.it.workstation.gui.browser.actions;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.OntologyTermReference;

import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.ImageModel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

/**
 * TBD
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RemoveAnnotationTermAction<T,S> implements NamedAction {

    private final ImageModel<T,S> imageModel;
    private final List<T> selectedObjects;
    
    private final OntologyTermReference keyRef;
    private final String annotationKeyName;

    public RemoveAnnotationTermAction(ImageModel<T,S> imageModel, List<T> selectedObjects, OntologyTermReference keyRef, String annotationKeyName) {
        this.imageModel = imageModel;
        this.selectedObjects = selectedObjects;
        this.keyRef = keyRef;
        this.annotationKeyName = annotationKeyName;
    }

    @Override
    public String getName() {
        return selectedObjects.size() > 1 ? "  Remove \"" + annotationKeyName + "\" Annotation From " + selectedObjects.size() + " Items" : "  Remove Annotation";
    }

    @Override
    public void doAction() {
    
        final DomainModel model = DomainMgr.getDomainMgr().getModel();

        if (selectedObjects.size() > 1) {
            int deleteConfirmation = JOptionPane.showConfirmDialog(SessionMgr.getMainFrame(), "Are you sure you want to delete this annotation from all selected items?", "Delete Annotations", JOptionPane.YES_NO_OPTION);
            if (deleteConfirmation != 0) {
                return;
            }
        }

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {

                List<Annotation> toRemove = new ArrayList<>();
                for (T selectedObject : selectedObjects) {
                    for (Annotation annotation : imageModel.getAnnotations(selectedObject)) {
                        if (annotation.getKeyTerm().getOntologyTermId().equals(keyRef.getOntologyTermId())) {
                            toRemove.add(annotation);            
                        }
                    }
                }
                
                int i = 1;
                for(Annotation annotation : toRemove) {
                    model.remove(annotation);    
                    setProgress(i++, toRemove.size());
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

        worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getMainFrame(), "Deleting Annotations", "", 0, 100));
        worker.execute();
    }
}
