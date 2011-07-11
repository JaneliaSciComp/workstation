/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/15/11
 * Time: 12:40 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.actions;

import javax.swing.JOptionPane;

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.FlyWorkstation.gui.framework.console.AnnotatedImageButton;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.types.*;
import org.janelia.it.jacs.model.ontology.types.Enum;

/**
 * This action creates and saves an annotation, and adds a corresponding tag to the currently selected item in an IconDemoPanel.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotateAction extends OntologyElementAction {

    @Override
    public void doAction() {
        ConsoleApp.getMainFrame().getOntologyOutline().navigateToOntologyElement(getOntologyElement());
        
        AnnotatedImageButton currImage = ConsoleApp.getMainFrame().getViewerPanel().getSelectedImage();
        
        if (currImage == null) {
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
            		ConsoleApp.getMainFrame(),
                    "Value:\n",
                    "Annotating with interval",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    null);
            
            Double dvalue = Double.parseDouble(value);
            
            Interval interval = (Interval)type;
            if (dvalue < interval.getLowerBound().doubleValue() || dvalue > interval.getUpperBound().doubleValue()) {
            	JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(), "Input out of range ["+interval.getLowerBound()+","+interval.getUpperBound()+"]");
            	return;
            }
        }
        else if (type instanceof Text) {
            value = (String) JOptionPane.showInputDialog(
            		ConsoleApp.getMainFrame(),
                    "Value:\n",
                    "Annotating with text",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    null);
        }

        
        // Create the visible tag
//        
//        String tag = null;
//        if (type instanceof EnumItem) {
//        	OntologyTerm parent = term.getParentTerm();
//        	tag = parent.getName()+" = "+term.getName();
//        }
//        else {
//            tag = (value == null) ? getName() : getName()+" = "+value;
//        }
//        
        // TODO: get entity from currImage
        Entity targetEntity = new Entity();
        targetEntity.setId(12345L);
        
        
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

        // TODO: get session id
        String sessionId = null;

        // TODO: check if annotation exists (do we need to delete it or replace it instead?)
        
        String tag = (valueString == null) ? keyString : keyString+" = "+valueString;
        boolean added = ConsoleApp.getMainFrame().getViewerPanel().addOrRemoveTag(tag);

        // TODO: add sanity check to verify that a tag was added 
        
        String keyEntityId = (keyEntity == null) ? null : keyEntity.getId().toString();
        String valueEntityId = (valueEntity == null) ? null : valueEntity.getId().toString();
        
		saveAnnotation(sessionId, targetEntity.getId().toString(), keyEntityId, keyString, 
				valueEntityId, valueString, tag);
	}

	private void saveAnnotation(final String sessionId, final String targetEntityId, final String keyEntityId,
			final String keyString, final String valueEntityId, final String valueString, final String tag) {

		SimpleWorker worker = new SimpleWorker() {

			private Entity newAnnot;

			protected void doStuff() throws Exception {
				newAnnot = EJBFactory.getRemoteAnnotationBean().createOntologyAnnotation(System.getenv("USER"),
						sessionId, targetEntityId, keyEntityId, keyString, valueEntityId, valueString, tag);
			}

			protected void hadSuccess() {
				// TODO: in the future maybe display the saved annotation somewhere?
				System.out.println("Saved annotation "+tag);
			}

			protected void hadError(Throwable error) {
		        ConsoleApp.getMainFrame().getViewerPanel().addOrRemoveTag(tag);
				error.printStackTrace();
				JOptionPane.showMessageDialog(ConsoleApp.getMainFrame().getViewerPanel(), "Error saving annotation",
						"Annotation Error", JOptionPane.ERROR_MESSAGE);
			}

		};
		worker.execute();
	}
	

	
	
	
	
}
