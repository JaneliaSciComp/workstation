package org.janelia.it.workstation.gui.framework.actions;

import org.janelia.it.workstation.gui.dialogs.AnnotationBuilderDialog;
import org.janelia.it.workstation.gui.framework.outline.Annotations;
import org.janelia.it.workstation.gui.framework.viewer.Viewer;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
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
public class BulkEditAnnotationKeyValueAction implements Action {

	private List<String> selectedEntities;
	private OntologyAnnotation tag;

    public BulkEditAnnotationKeyValueAction(OntologyAnnotation tag) {
        selectedEntities = new ArrayList<String>(
                org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(
                        org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getBrowser().getViewerManager().getActiveViewer().getSelectionCategory()));
        this.tag = tag;
    }

    @Override
    public String getName() {
    	return selectedEntities.size()>1?"Edit \""+tag.toString()+"\" Annotation On "+selectedEntities.size()+" Entities":"Edit Annotation";
    }
	
    @Override
    public void doAction() {
        try {
        	// TODO: this should really use the ModelMgr
        	final Viewer viewer = org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getBrowser().getViewerManager().getActiveViewer();
        	if (viewer instanceof org.janelia.it.workstation.gui.framework.viewer.IconDemoPanel) {
        		org.janelia.it.workstation.gui.framework.viewer.IconDemoPanel iconDemoPanel = (org.janelia.it.workstation.gui.framework.viewer.IconDemoPanel)viewer;
            	final Annotations annotations = iconDemoPanel.getAnnotations();
                final Map<Long, List<OntologyAnnotation>> annotationMap = annotations.getFilteredAnnotationMap();
                
                SimpleWorker worker = new SimpleWorker() {
                    @Override
                    protected void doStuff() throws Exception {
                        AnnotationBuilderDialog dialog = new AnnotationBuilderDialog();
                        dialog.setAnnotationValue(tag.getValueString());
                        dialog.setVisible(true);
                        String value = dialog.getAnnotationValue();
                        String originalAnnotationValue = dialog.getOriginalAnnotationText();
                        if (null==value) { value=""; }
                        if (value.equals(originalAnnotationValue)) {
                            System.out.println("Doing nothing.  The annotation was not changed.");
                            return;
                        }
                        try {
                            int i=1;
                            for(String selectedId : selectedEntities) {
                                org.janelia.it.workstation.model.entity.RootedEntity rootedEntity = viewer.getRootedEntityById(selectedId);
                                List<OntologyAnnotation> entityAnnotations = annotationMap.get(rootedEntity.getEntity().getId());
                                if (entityAnnotations==null) {
                                    continue;
                                }
                                for(OntologyAnnotation annotation : entityAnnotations) {
                                    if (annotation.getValueString().equals(originalAnnotationValue)) {
                                        annotation.setValueString(value);
                                        annotation.getEntity().setValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_TERM, value);
                                        String tmpName = annotation.getEntity().getName();
                                        String namePrefix = tmpName.substring(0,tmpName.indexOf("=")+2);
                                        annotation.getEntity().setName(namePrefix+value);
                                        try {
                                            Entity tmpAnnotatedEntity = org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().getEntityById(annotation.getTargetEntityId());
                                            org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().saveOrUpdateAnnotation(tmpAnnotatedEntity, annotation.getEntity());
                                        }
                                        catch (Exception e1) {
                                            e1.printStackTrace();
                                            org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().handleException(e1);
                                        }
                                    }
                                }
                                setProgress(i++, selectedEntities.size());
                            }
                        }
                        catch (Exception e1) {
                            e1.printStackTrace();
                            org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().handleException(e1);
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

                worker.setProgressMonitor(new ProgressMonitor(org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getMainFrame(), "Editing Annotations", "", 0, 100));
                worker.execute();
        	}
        }
        catch (Exception ex) {
        	org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().handleException(ex);
        }
    	
    }
}
