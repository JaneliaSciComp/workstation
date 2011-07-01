/*
 * Created by IntelliJ IDEA.
 * User: rokickik
 * Date: 6/15/11
 * Time: 12:40 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.actions;

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.janelia.it.FlyWorkstation.gui.framework.outline.OntologyTerm;
import org.janelia.it.jacs.model.ontology.*;
import org.janelia.it.jacs.model.ontology.Enum;

import javax.swing.*;

/**
 * This action adds or removes an entity tag from the currently selected item in an IconDemoPanel.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotateAction extends OntologyTermAction {

    public AnnotateAction(OntologyTerm term) {
        super(term);
    }

    @Override
    public void doAction() {
        ConsoleApp.getMainFrame().getOntologyOutline().navigateToOntologyTerm(getOntologyTerm());
        OntologyTerm term = getOntologyTerm();
        OntologyTermType type = term.getType();
        
        if (type instanceof Category || type instanceof Enum) {
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
        
        // TODO: create the annotation
        
        // Create the visible tag
        
        String tag = null;
        if (type instanceof EnumItem) {
        	OntologyTerm parent = term.getParentTerm();
        	tag = parent.getName()+" = "+term.getName();
        }
        else {
            tag = (value == null) ? getName() : getName()+" = "+value;
        }
        
        ConsoleApp.getMainFrame().getViewerPanel().addOrRemoveTag(tag);
    }

}
