package org.janelia.it.FlyWorkstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.swing.*;

import loci.plugins.config.SpringUtilities;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.AnnotationSession;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;

/**
 * A dialog for viewing details about an ontological annotation.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationDetailsDialog extends ModalDialog {

    protected static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    
    private JPanel attrPanel;
    private JLabel sessionLabel;
    private JLabel sessionOwnerLabel;
    private JLabel keyLabel;
    private JLabel valueLabel;
    private JLabel ownerLabel;
    private JLabel creationDateLabel;

    private JLabel addAttribute(String name) {
        JLabel nameLabel = new JLabel(name);
        JLabel valueLabel = new JLabel();
        nameLabel.setLabelFor(valueLabel);
        attrPanel.add(nameLabel);
        attrPanel.add(valueLabel);
        return valueLabel;
    }
    
    public AnnotationDetailsDialog() {

        setTitle("Annotation Details");

        attrPanel = new JPanel(new SpringLayout());
        attrPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10), 
        		BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Annotation Properties")));

        sessionLabel = addAttribute("Session: ");
        sessionOwnerLabel = addAttribute("Session Owner: ");
        keyLabel = addAttribute("Annotation Key: ");
        valueLabel = addAttribute("Annotation Value: ");
        ownerLabel = addAttribute("Annotation Owner: ");
        creationDateLabel = addAttribute("Creation Date: ");

        add(attrPanel, BorderLayout.CENTER);
        SpringUtilities.makeCompactGrid(attrPanel, attrPanel.getComponentCount()/2, 2, 6, 6, 6, 6);

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            setVisible(false);
			}
		});

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);
        
        add(buttonPane, BorderLayout.SOUTH);
    }
    
    public void showForAnnotation(OntologyAnnotation annotation) {

    	keyLabel.setText(annotation.getKeyString());
    	valueLabel.setText(annotation.getValueString());
        ownerLabel.setText(annotation.getOwner());
        creationDateLabel.setText(df.format(annotation.getEntity().getCreationDate()));
        
    	try {
    		AnnotationSession session = annotation.getSessionId()==null?null:ModelMgr.getModelMgr().getAnnotationSession(annotation.getSessionId());	
    		sessionLabel.setText(session == null ? "None": session.getName());
    		sessionOwnerLabel.setText(session == null ? "": session.getOwner());
    	}
    	catch (Exception e) {
    		SessionMgr.getSessionMgr().handleException(e);
    	}

        packAndShow();
    }
}
