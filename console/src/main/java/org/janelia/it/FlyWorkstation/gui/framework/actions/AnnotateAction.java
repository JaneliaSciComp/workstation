package org.janelia.it.FlyWorkstation.gui.framework.actions;

import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.dialogs.AnnotationBuilderDialog;
import org.janelia.it.FlyWorkstation.gui.framework.outline.AnnotationSession;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.types.*;
import org.janelia.it.jacs.model.ontology.types.Enum;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * This action creates and saves an annotation, and adds a corresponding tag to the currently selected item in an IconDemoPanel.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotateAction extends OntologyElementAction {
	
    Callable<Void> doSuccess = null;

    public AnnotateAction() {
    }

    public AnnotateAction(Callable<Void> doSuccess) {
        this.doSuccess = doSuccess;
    }

    @Override
    public void doAction() {
        SessionMgr.getSessionMgr().getActiveBrowser().getOntologyOutline().navigateToOntologyElement(getOntologyElement());
        final List<RootedEntity> selectedEntities = SessionMgr.getBrowser().getViewerManager().getActiveViewer().getSelectedEntities();
        
        if (selectedEntities.isEmpty()) {
            // Cannot annotate nothing
            System.out.println("AnnotateAction called without an entity being selected");
            return;
        }

        final OntologyElement term = getOntologyElement();
        final OntologyElementType type = term.getType();

        if (type instanceof Category || type instanceof Enum) {
            // Cannot annotate with a category or enum
            return;
        }

        // Get the input value, if required

        Object value = null;
        if (type instanceof Interval) {
            value = JOptionPane.showInputDialog(SessionMgr.getSessionMgr().getActiveBrowser(), 
            		"Value:\n", term.getName(), JOptionPane.PLAIN_MESSAGE, null, null, null);

            if (StringUtils.isEmpty((String)value)) return;
            Double dvalue = Double.parseDouble((String)value);
            Interval interval = (Interval) type;
            if (dvalue < interval.getLowerBound().doubleValue() || dvalue > interval.getUpperBound().doubleValue()) {
                JOptionPane.showMessageDialog(SessionMgr.getSessionMgr().getActiveBrowser(), 
                		"Input out of range [" + interval.getLowerBound() + "," + interval.getUpperBound() + "]");
                return;
            }
        }
        else if (type instanceof EnumText) {
        	
        	OntologyElement valueEnum = ((EnumText) type).getValueEnum();
        	
        	if (valueEnum==null) {
        		Exception error = new Exception(term.getName()+" has no supporting enumeration.");
				SessionMgr.getSessionMgr().handleException(error);
        		return;
        	}
        	
        	List<OntologyElement> children = valueEnum.getChildren();
        	
        	int i = 0;
        	Object[] selectionValues = new Object[children.size()];
        	for(OntologyElement child : children) {
        		selectionValues[i++] = child;
        	}
        	
        	value = JOptionPane.showInputDialog(SessionMgr.getSessionMgr().getActiveBrowser(), 
            		"Value:\n", term.getName(), JOptionPane.PLAIN_MESSAGE, null, selectionValues, null);
        	if (value==null) return;
        }
        else if (type instanceof Text) {
            AnnotationBuilderDialog dialog = new AnnotationBuilderDialog();
            dialog.setVisible(true);
            value = dialog.getPathString();
//            value = JOptionPane.showInputDialog(SessionMgr.getSessionMgr().getActiveBrowser(),
//            		"Value:\n", term.getName(), JOptionPane.PLAIN_MESSAGE, null, null, null);
            if (value==null || value.equals("")) return;
        }
        
        final OntologyElement finalTerm = term;
        final Object finalValue = value;
        
        SimpleWorker worker = new SimpleWorker() {

			@Override
			protected void doStuff() throws Exception {
				int i=1;
		        for(RootedEntity rootedEntity : selectedEntities) {
		        	Entity entity = rootedEntity.getEntity();
		        	doAnnotation(entity, finalTerm, finalValue);
		            setProgress(i++, selectedEntities.size());
		        }
			}

			@Override
			protected void hadSuccess() {
				if (doSuccess != null) {
                    try {
                        doSuccess.call();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
			}

			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
        	
        };

        worker.setProgressMonitor(new ProgressMonitor(SessionMgr.getSessionMgr().getActiveBrowser(), "Adding annotations", "", 0, 100));
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

        Long keyEntityId = (keyEntity == null) ? null : keyEntity.getId();
        Long valueEntityId = (valueEntity == null) ? null : valueEntity.getId();

        final OntologyAnnotation annotation = new OntologyAnnotation(
        		sessionId, targetEntity.getId(), keyEntityId, keyString, valueEntityId, valueString);

        Entity annotationEntity = ModelMgr.getModelMgr().createOntologyAnnotation(annotation);
        System.out.println("Saved annotation as " + annotationEntity.getId());
    }
}
