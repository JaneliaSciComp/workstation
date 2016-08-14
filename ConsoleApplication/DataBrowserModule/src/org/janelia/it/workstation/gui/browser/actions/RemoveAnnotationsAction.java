package org.janelia.it.workstation.gui.browser.actions;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import org.apache.commons.lang.StringUtils;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.workstation.gui.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.gui.listview.icongrid.ImageModel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

/**
 * Remove annotations from multiple domain objects.
 * 
 * This action works in two modes, depending on the value of matchIdOrName:
 * 1) True: matches the annotation name, or id (if only one object is selected)
 * 2) False: matches the annotation's key id
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RemoveAnnotationsAction implements NamedAction {

    private final ImageModel<DomainObject,Reference> imageModel;
    private final List<DomainObject> selectedObjects;
    private Annotation annotation;
    private boolean matchIdOrName = false;

    public RemoveAnnotationsAction(ImageModel<DomainObject,Reference> imageModel, List<DomainObject> selectedObjects, Annotation annotation, boolean matchIdOrName) {
        this.imageModel = imageModel;
        this.selectedObjects = selectedObjects;
        this.annotation = annotation;
        this.matchIdOrName = matchIdOrName;
    }

    @Override
    public String getName() {
        String target = matchIdOrName ? annotation.getName() : annotation.getKey();
        return selectedObjects.size() > 1 ? "Remove \"" + target + "\" Annotation From " + selectedObjects.size() + " Items" : "Remove Annotation";
    }

    @Override
    public void doAction() {

        ActivityLogHelper.logUserAction("RemoveAnnotationsAction.doAction", annotation);

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

                if (selectedObjects.size()==1 && matchIdOrName) {
                    model.remove(annotation);
                    return;
                }
                
                List<Annotation> toRemove = new ArrayList<>();
                for (DomainObject selectedObject : selectedObjects) {
                    for (Annotation annotation : imageModel.getAnnotations(selectedObject)) {
                        if (matchIdOrName && StringUtils.equals(annotation.getName(), RemoveAnnotationsAction.this.annotation.getName())) {
                            toRemove.add(annotation);            
                        }
                        else if (!matchIdOrName && annotation.getKeyTerm().getOntologyTermId().equals(RemoveAnnotationsAction.this.annotation.getKeyTerm().getOntologyTermId())) {
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
