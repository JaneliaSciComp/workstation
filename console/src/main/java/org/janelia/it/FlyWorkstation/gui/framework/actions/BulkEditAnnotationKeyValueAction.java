package org.janelia.it.FlyWorkstation.gui.framework.actions;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.AnnotationBuilderDialog;
import org.janelia.it.FlyWorkstation.gui.framework.outline.Annotations;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.IconDemoPanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.Viewer;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
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
                ModelMgr.getModelMgr().getEntitySelectionModel().getSelectedEntitiesIds(
                        SessionMgr.getBrowser().getViewerManager().getActiveViewer().getSelectionCategory()));
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
        	final Viewer viewer = SessionMgr.getBrowser().getViewerManager().getActiveViewer();
        	if (viewer instanceof IconDemoPanel) {
        		IconDemoPanel iconDemoPanel = (IconDemoPanel)viewer;
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
                                RootedEntity rootedEntity = viewer.getRootedEntityById(selectedId);
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
                                            Entity tmpAnnotatedEntity = ModelMgr.getModelMgr().getEntityById(annotation.getTargetEntityId());
                                            ModelMgr.getModelMgr().saveOrUpdateAnnotation(tmpAnnotatedEntity, annotation.getEntity());
                                        }
                                        catch (Exception e1) {
                                            e1.printStackTrace();
                                            SessionMgr.getSessionMgr().handleException(e1);
                                        }
                                    }
                                }
                                setProgress(i++, selectedEntities.size());
                            }
                        }
                        catch (Exception e1) {
                            e1.printStackTrace();
                            SessionMgr.getSessionMgr().handleException(e1);
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

                worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getBrowser(), "Editing Annotations", "", 0, 100));
                worker.execute();
        	}
        }
        catch (Exception ex) {
        	SessionMgr.getSessionMgr().handleException(ex);
        }
    	
    }
}
