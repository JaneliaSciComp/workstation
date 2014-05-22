package org.janelia.it.workstation.gui.framework.actions;

import org.janelia.it.jacs.model.ontology.OntologyAnnotation;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This action removes an entity from some parent. If the entity becomes an orphan, then it is completely deleted.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RemoveAnnotationTermAction implements Action {

	private List<String> selectedEntities;
	private String annotationKeyName;
    private Long keyEntityId;

	public RemoveAnnotationTermAction(Long keyTermId, String keyString) {
    	selectedEntities = new ArrayList<String>(
    			org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(
    					org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getBrowser().getViewerManager().getActiveViewer().getSelectionCategory()));
    	this.annotationKeyName = keyString;
		this.keyEntityId = keyTermId;
	}
	
    @Override
    public String getName() {
    	return selectedEntities.size()>1?"  Remove \""+annotationKeyName+"\" Annotation From "+selectedEntities.size()+" Entities":"  Remove Annotation";
    }
	
    @Override
    public void doAction() {

    	if (selectedEntities.size()>1) {
            int deleteConfirmation = JOptionPane.showConfirmDialog(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getMainFrame(), "Are you sure you want to delete this annotation from all selected entities?", "Delete Annotations", JOptionPane.YES_NO_OPTION);
            if (deleteConfirmation != 0) {
                return;
            }
    	}

        try {
        	
        	// TODO: this should really use the ModelMgr
        	final org.janelia.it.workstation.gui.framework.viewer.Viewer viewer = org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getBrowser().getViewerManager().getActiveViewer();
        	if (viewer instanceof org.janelia.it.workstation.gui.framework.viewer.IconDemoPanel) {
        		org.janelia.it.workstation.gui.framework.viewer.IconDemoPanel iconDemoPanel = (org.janelia.it.workstation.gui.framework.viewer.IconDemoPanel)viewer;
            	final org.janelia.it.workstation.gui.framework.outline.Annotations annotations = iconDemoPanel.getAnnotations();
                final Map<Long, List<OntologyAnnotation>> annotationMap = annotations.getFilteredAnnotationMap();
                
                org.janelia.it.workstation.shared.workers.SimpleWorker worker = new org.janelia.it.workstation.shared.workers.SimpleWorker() {

                    @Override
                    protected void doStuff() throws Exception {
                        

                        int i=1;
            			for(String selectedId : selectedEntities) {
            				org.janelia.it.workstation.model.entity.RootedEntity rootedEntity = viewer.getRootedEntityById(selectedId);
                            List<OntologyAnnotation> entityAnnotations = annotationMap.get(rootedEntity.getEntity().getId());
                            if (entityAnnotations==null) {
                            	continue;
                            }
                            for(OntologyAnnotation annotation : entityAnnotations) {
                            	if (annotation.getKeyEntityId().equals(keyEntityId)) {
                            		org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().removeAnnotation(annotation.getId());
                            	}
                            }
        		            setProgress(i++, selectedEntities.size());
                    	}
                    }

                    @Override
                    protected void hadSuccess() {
        				// No need to do anything
                    }

                    @Override
                    protected void hadError(Throwable error) {
                        org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().handleException(error);
                    }
                };

                worker.setProgressMonitor(new ProgressMonitor(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getMainFrame(), "Deleting Annotations", "", 0, 100));
                worker.execute();
        	}
        }
        catch (Exception ex) {
        	org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().handleException(ex);
        }
    	
    }
}
