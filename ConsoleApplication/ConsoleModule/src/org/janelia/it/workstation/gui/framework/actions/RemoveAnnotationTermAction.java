package org.janelia.it.workstation.gui.framework.actions;

import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.outline.Annotations;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * This action removes an entity from some parent. If the entity becomes an orphan, then it is completely deleted.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RemoveAnnotationTermAction implements Action {

    private final List<String> selectedEntities;
    private final String annotationKeyName;
    private final Long keyEntityId;

    public RemoveAnnotationTermAction(Long keyTermId, String keyString) {
        selectedEntities = new ArrayList<>(
                ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(
                        SessionMgr.getBrowser().getViewerManager().getActiveViewer().getSelectionCategory()));
        this.annotationKeyName = keyString;
        this.keyEntityId = keyTermId;
    }

    @Override
    public String getName() {
        return selectedEntities.size() > 1 ? "  Remove \"" + annotationKeyName + "\" Annotation From " + selectedEntities.size() + " Items" : "  Remove Annotation";
    }

    @Override
    public void doAction() {

        if (selectedEntities.size() > 1) {
            int deleteConfirmation = JOptionPane.showConfirmDialog(SessionMgr.getMainFrame(), "Are you sure you want to delete this annotation from all selected items?", "Delete Annotations", JOptionPane.YES_NO_OPTION);
            if (deleteConfirmation != 0) {
                return;
            }
        }

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {

                List<Long> entityIds = new ArrayList<>();
                for (String selectedId : selectedEntities) {
                    entityIds.add(EntityUtils.getEntityIdFromUniqueId(selectedId));
                }
                
                Annotations annotations = new Annotations();
                annotations.init(entityIds);
                List<OntologyAnnotation> annotationList = annotations.getAnnotations();
        
                int i = 1;
                for (OntologyAnnotation annotation : annotationList) {
                    if (annotation.getKeyEntityId().equals(keyEntityId)) {
                        ModelMgr.getModelMgr().removeAnnotation(annotation.getId());
                    }
                    setProgress(i++, annotationList.size());
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
