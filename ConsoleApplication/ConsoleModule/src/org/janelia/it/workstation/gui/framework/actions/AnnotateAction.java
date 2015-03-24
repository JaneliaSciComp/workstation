package org.janelia.it.workstation.gui.framework.actions;

import java.util.ArrayList;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.types.*;
import org.janelia.it.jacs.model.ontology.types.Enum;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.dialogs.AnnotationBuilderDialog;
import org.janelia.it.workstation.gui.framework.outline.OntologyOutline;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.model.utils.AnnotationSession;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.Callable;
import org.janelia.it.jacs.model.util.PermissionTemplate;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.workstation.api.entity_model.management.EntitySelectionModel;

/**
 * This action creates and saves an annotation, and adds a corresponding tag to the currently selected item in an IconDemoPanel.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotateAction extends OntologyElementAction {
	
	private static final Logger log = LoggerFactory.getLogger(AnnotateAction.class);
	
    private Callable<Void> doSuccess = null;

    public AnnotateAction() {
    }

    public AnnotateAction(Callable<Void> doSuccess) {
        this.doSuccess = doSuccess;
    }

    @Override
    public void doAction() {
        
        final OntologyOutline ontologyOutline = SessionMgr.getBrowser().getOntologyOutline();
        
        OntologyElement element = getOntologyElement();
        if (element==null) {
            EntityData entityTermEd = ontologyOutline.getEntityDataByUniqueId(getUniqueId());
            if (entityTermEd==null) {
                throw new IllegalStateException("Cannot find entity data with unique id: "+getUniqueId());
            }
            element = ontologyOutline.getOntologyElement(entityTermEd);
        }
        
        ontologyOutline.navigateToOntologyElement(element);
        
        EntitySelectionModel esm = ModelMgr.getModelMgr().getEntitySelectionModel();
        final List<String> selectedEntityIds = esm.getSelectedEntitiesIds(esm.getActiveCategory());
        
        if (selectedEntityIds.isEmpty()) {
            // Cannot annotate nothing
            log.warn("AnnotateAction called without an entity being selected");
            return;
        }

        final List<Entity> selectedEntities = new ArrayList<>();
        for(String selectedEntityId : selectedEntityIds) {
            try {
                selectedEntities.add(ModelMgr.getModelMgr().getEntityById(EntityUtils.getEntityIdFromUniqueId(selectedEntityId)));    
            }
            catch (Exception e) {
                SessionMgr.getSessionMgr().handleException(e);
            }
        }
        
        final OntologyElementType type = element.getType();

        if (type instanceof Category || type instanceof Enum) {
            // Cannot annotate with a category or enum
            return;
        }

        // Get the input value, if required

        Object value = null;
        if (type instanceof Interval) {
            value = JOptionPane.showInputDialog(SessionMgr.getMainFrame(), 
            		"Value:\n", element.getName(), JOptionPane.PLAIN_MESSAGE, null, null, null);

            if (StringUtils.isEmpty((String)value)) return;
            Double dvalue = Double.parseDouble((String)value);
            Interval interval = (Interval) type;
            if (dvalue < interval.getLowerBound().doubleValue() || dvalue > interval.getUpperBound().doubleValue()) {
                JOptionPane.showMessageDialog(SessionMgr.getMainFrame(), 
                		"Input out of range [" + interval.getLowerBound() + "," + interval.getUpperBound() + "]");
                return;
            }
        }
        else if (type instanceof EnumText) {
        	
        	OntologyElement valueEnum = ((EnumText) type).getValueEnum();
        	
        	if (valueEnum==null) {
        		Exception error = new Exception(element.getName()+" has no supporting enumeration.");
				SessionMgr.getSessionMgr().handleException(error);
        		return;
        	}
        	
        	List<OntologyElement> children = valueEnum.getChildren();
        	
        	int i = 0;
        	Object[] selectionValues = new Object[children.size()];
        	for(OntologyElement child : children) {
        		selectionValues[i++] = child;
        	}
        	
        	value = JOptionPane.showInputDialog(SessionMgr.getMainFrame(), 
            		"Value:\n", element.getName(), JOptionPane.PLAIN_MESSAGE, null, selectionValues, null);
        	if (value==null) return;
        }
        else if (type instanceof Text) {
            AnnotationBuilderDialog dialog = new AnnotationBuilderDialog();
            dialog.setVisible(true);
            value = dialog.getAnnotationValue();
//            value = JOptionPane.showInputDialog(SessionMgr.getBrowser(),
//            		"Value:\n", term.getName(), JOptionPane.PLAIN_MESSAGE, null, null, null);
            if (value==null || value.equals("")) return;
        }
        
        final OntologyElement finalTerm = element;
        final Object finalValue = value;

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                int i = 1;
                for (Entity entity : selectedEntities) {
                    if (EntityUtils.isVirtual(entity)) continue;
                    doAnnotation(entity, finalTerm, finalValue);
                    setProgress(i++, selectedEntities.size());
                }
            }

            @Override
            protected void hadSuccess() {
                ConcurrentUtils.invokeAndHandleExceptions(doSuccess);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
            }

        };

        worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getMainFrame(), "Adding annotations", "", 0, 100));
        worker.execute();
    }
    
    public void doAnnotation(Entity targetEntity, OntologyElement term, Object value) throws Exception {

        OntologyElementType type = term.getType();
        
        // Save the annotation
        Entity keyEntity = term.getEntity();
        Entity valueEntity = null;
        String keyString = keyEntity.getName();
        String valueString = value == null ? null : value.toString();

        if (type instanceof EnumItem) {
            keyEntity = term.getParent().getEntity();
            valueEntity = term.getEntity();
            keyString = keyEntity.getName();
            valueString = valueEntity.getName();
        }

        AnnotationSession session = ModelMgr.getModelMgr().getCurrentAnnotationSession();
        Long sessionId = (null != session) ? session.getId() : null;

        Long keyEntityId = keyEntity.getId();
        Long valueEntityId = (valueEntity == null) ? null : valueEntity.getId();

        final OntologyAnnotation annotation = new OntologyAnnotation(
        		sessionId, targetEntity.getId(), keyEntityId, keyString, valueEntityId, valueString);

        createAndShareAnnotation(annotation);
    }
    
    private void createAndShareAnnotation(OntologyAnnotation annotation) throws Exception {
        
        Entity annotationEntity = ModelMgr.getModelMgr().createOntologyAnnotation(annotation);
        log.info("Saved annotation as " + annotationEntity.getId());
        
        PermissionTemplate template = SessionMgr.getBrowser().getAutoShareTemplate();
        if (template!=null) {
            ModelMgr.getModelMgr().grantPermissions(annotationEntity.getId(), template.getSubjectKey(), template.getPermissions(), false);
            log.info("Auto-shared annotation with " + template.getSubjectKey());
        }
    }
}
