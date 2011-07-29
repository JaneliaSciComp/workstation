/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/15/11
 * Time: 12:40 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.actions;

import javax.swing.JOptionPane;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.types.*;
import org.janelia.it.jacs.model.ontology.types.Enum;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * This action creates and saves an annotation, and adds a corresponding tag to the currently selected item in an IconDemoPanel.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotateAction extends OntologyElementAction {
	
    @Override
    public void doAction() {
        SessionMgr.getSessionMgr().getActiveBrowser().getOntologyOutline().navigateToOntologyElement(getOntologyElement());
        
        Entity targetEntity = SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel().getCurrentEntity();
        
        if (targetEntity == null) {
        	// Cannot annotate nothing
        	return;
        }
        
        OntologyElement term = getOntologyElement();
        OntologyElementType type = term.getType();
        
        if (type instanceof Category || type instanceof Enum) {
        	// Cannot annotate with a category or enum
        	return;
        }
        
        // Get the input value, if required
        
        String value = null;
        if (type instanceof Interval) {
            value = (String) JOptionPane.showInputDialog(
            		SessionMgr.getSessionMgr().getActiveBrowser(),
                    "Value:\n",
                    "Annotating with interval",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    null);
            
            Double dvalue = Double.parseDouble(value);
            
            Interval interval = (Interval)type;
            if (dvalue < interval.getLowerBound().doubleValue() || dvalue > interval.getUpperBound().doubleValue()) {
            	JOptionPane.showMessageDialog(SessionMgr.getSessionMgr().getActiveBrowser(), "Input out of range ["+interval.getLowerBound()+","+interval.getUpperBound()+"]");
            	return;
            }
        }
        else if (type instanceof Text) {
            value = (String) JOptionPane.showInputDialog(
            		SessionMgr.getSessionMgr().getActiveBrowser(),
                    "Value:\n",
                    "Annotating with text",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    null);
        }

        // Save the annotation
        Entity keyEntity = term.getEntity();
        Entity valueEntity = null;
    	String keyString = keyEntity.getName();
    	String valueString = value;
    	
        if (type instanceof EnumItem) {
        	keyEntity = term.getParent().getEntity();
        	valueEntity = term.getEntity();
        	keyString = keyEntity.getName();
        	valueString = valueEntity.getName();
        }

        Task sessionId = ModelMgr.getModelMgr().getCurrentAnnotationSessionTask();
        String sessionIdString = (null!=sessionId)?sessionId.getObjectId().toString():null;
        
        String tag = (valueString == null) ? keyString : keyString+" = "+valueString;
        String keyEntityId = (keyEntity == null) ? null : keyEntity.getId().toString();
        String valueEntityId = (valueEntity == null) ? null : valueEntity.getId().toString();
        
        saveAnnotation(sessionIdString, targetEntity, keyEntityId, keyString,
			valueEntityId, valueString, tag);
        
	}

    private void saveAnnotation(final String sessionId, final Entity targetEntity, final String keyEntityId,
			final String keyString, final String valueEntityId, final String valueString, final String tag) {
    	
    	Utils.setWaitingCursor(SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel());
    	
		SimpleWorker worker = new SimpleWorker() {

			protected void doStuff() throws Exception {
		        // TODO: check if annotation exists 
				EJBFactory.getRemoteAnnotationBean().createOntologyAnnotation((String)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME),
						sessionId, targetEntity.getId().toString(), keyEntityId, keyString, valueEntityId, valueString, tag);
			}

			protected void hadSuccess() { 
				System.out.println("Saved annotation "+tag);
		        SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel().refreshEntity(targetEntity);
				
			}

			protected void hadError(Throwable error) {
				error.printStackTrace();
				JOptionPane.showMessageDialog(SessionMgr.getSessionMgr().getActiveBrowser().getViewerPanel(), "Error saving annotation",
						"Annotation Error", JOptionPane.ERROR_MESSAGE);
			}

		};
		worker.execute();
	}
}
