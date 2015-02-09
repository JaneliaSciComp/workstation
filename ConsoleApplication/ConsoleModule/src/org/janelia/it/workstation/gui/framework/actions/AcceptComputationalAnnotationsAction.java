package org.janelia.it.workstation.gui.framework.actions;

import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.framework.outline.Annotations;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.viewer.IconDemoPanel;
import org.janelia.it.workstation.gui.framework.viewer.Viewer;
import org.janelia.it.workstation.model.entity.RootedEntity;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This action accepts all computational annotations on an entity or multiple entities, "curating" them into normal annotations. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AcceptComputationalAnnotationsAction implements Action {

	private List<String> selectedEntities;

    public AcceptComputationalAnnotationsAction() {
        selectedEntities = new ArrayList<String>(
                ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(
                        SessionMgr.getBrowser().getViewerManager().getActiveViewer().getSelectionCategory()));
    }

    @Override
    public String getName() {
    	return selectedEntities.size()>1?"  Accept All Computational Annotations on "+selectedEntities.size()+" Items":"  Accept All Computational Annotations on This Item";
    }
	
    @Override
    public void doAction() {

    	if (selectedEntities.size()>1) {
            int acceptConfirmation = JOptionPane.showConfirmDialog(SessionMgr.getMainFrame(), "Are you sure you want to accept all computational annotations on all selected entities?", "Accept Annotations", JOptionPane.YES_NO_OPTION);
            if (acceptConfirmation != 0) {
                return;
            }
    	}

        try {
        	
        	// TODO: this should really use the ModelMgr
        	final Viewer viewer = SessionMgr.getBrowser().getViewerManager().getActiveViewer();
        	if (viewer instanceof IconDemoPanel) {
        		IconDemoPanel iconDemoPanel = (IconDemoPanel)viewer;
            	final Annotations annotations = iconDemoPanel.getAnnotations();
                final Map<Long, List<OntologyAnnotation>> annotationMap = annotations.getFilteredAnnotationMap();
                
                SimpleWorker worker = new SimpleWorker() {

                    @Override
                    protected void doStuff() throws Exception {
                        int i=1;
            			for(String selectedId : selectedEntities) {
            				RootedEntity rootedEntity = viewer.getRootedEntityById(selectedId);
                            List<OntologyAnnotation> entityAnnotations = annotationMap.get(rootedEntity.getEntity().getId());
                            if (entityAnnotations==null) {
                            	continue;
                            }
                    		ModelMgr.getModelMgr().acceptComputationAnnotation(rootedEntity.getEntity().getId(), entityAnnotations);
        		            setProgress(i++, selectedEntities.size());
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

                worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getMainFrame(), "Accepting Annotations", "", 0, 100));
                worker.execute();
        	}
        }
        catch (Exception ex) {
        	SessionMgr.getSessionMgr().handleException(ex);
        }
    	
    }
}
