package org.janelia.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import org.apache.commons.lang.StringUtils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.common.gui.model.ImageModel;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;

/**
 * Remove annotations from multiple domain objects.
 * 
 * This action works in two modes, depending on the value of matchIdOrName:
 * 1) True: matches the annotation name, or id (if only one object is selected)
 * 2) False: matches the annotation's key id
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RemoveAnnotationsAction extends AbstractAction {

    private final ImageModel<DomainObject,Reference> imageModel;
    private final List<DomainObject> selectedObjects;
    private Annotation annotation;
    private boolean matchIdOrName;

    public RemoveAnnotationsAction(ImageModel<DomainObject,Reference> imageModel, List<DomainObject> selectedObjects, Annotation annotation, boolean matchIdOrName) {
        super(getName(selectedObjects, annotation, matchIdOrName));
        this.imageModel = imageModel;
        this.selectedObjects = selectedObjects;
        this.annotation = annotation;
        this.matchIdOrName = matchIdOrName;
    }

    public static String getName(List<DomainObject> selectedObjects, Annotation annotation, boolean matchIdOrName) {
        String target = matchIdOrName ? annotation.getName() : annotation.getKey();
        return selectedObjects.size() > 1 ? "Remove \"" + target + "\" Annotation From " + selectedObjects.size() + " Items" : "Remove Annotation";
    }    

    @Override
    public void actionPerformed(ActionEvent event) {

        ActivityLogHelper.logUserAction("RemoveAnnotationsAction.doAction", annotation);

        final DomainModel model = DomainMgr.getDomainMgr().getModel();

        if (selectedObjects.size() > 1) {
            int deleteConfirmation = JOptionPane.showConfirmDialog(FrameworkAccess.getMainFrame(), "Are you sure you want to delete this annotation from all selected items?", "Delete Annotations", JOptionPane.YES_NO_OPTION);
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
                        if (ClientDomainUtils.hasWriteAccess(annotation)) {
                            if (matchIdOrName && StringUtils.equals(annotation.getName(), RemoveAnnotationsAction.this.annotation.getName())) {
                                toRemove.add(annotation);            
                            }
                            else if (!matchIdOrName && annotation.getKeyTerm().getOntologyTermId().equals(RemoveAnnotationsAction.this.annotation.getKeyTerm().getOntologyTermId())) {
                                toRemove.add(annotation);            
                            }
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
                FrameworkAccess.handleException(error);
            }
        };

        worker.setProgressMonitor(new ProgressMonitor(FrameworkAccess.getMainFrame(), "Deleting Annotations", "", 0, 100));
        worker.execute();
    }
}
