package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.outline.AnnotationSession;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;

/**
 * A dialog for viewing details about an ontological annotation.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationDetailsDialog extends JDialog {

    private static final String CLICKED_OK = "clicked_ok";
    
    protected static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    
    private JPanel attrPanel;
    
    private JLabel sessionLabel;
    private JLabel sessionOwnerLabel;
    private JLabel keyLabel;
    private JLabel valueLabel;
    private JLabel ownerLabel;
    private JLabel creationDateLabel;

    private JLabel addAttribute(String name) {

    	int row = attrPanel.getComponentCount()/2;
        GridBagConstraints c = new GridBagConstraints();
        
        JLabel nameLabel = new JLabel(name);
        c.gridx = 0;
        c.gridy = row;
        c.insets = new Insets(0, 0, 10, 0);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        attrPanel.add(nameLabel, c);

        JLabel valueLabel = new JLabel();
        c.gridx = 1;
        c.gridy = row;
        c.insets = new Insets(0, 0, 10, 0);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        attrPanel.add(valueLabel, c);
        
        return valueLabel;
    }
    
    public AnnotationDetailsDialog() {

        setTitle("Annotation Details");
        setSize(400, 300);
        getContentPane().setLayout(new BorderLayout());

        setLocationRelativeTo(SessionMgr.getSessionMgr().getActiveBrowser());
        
        attrPanel = new JPanel(new GridBagLayout());
        attrPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10), 
        		BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Annotation Properties")));

        sessionLabel = addAttribute("Session: ");
        sessionOwnerLabel = addAttribute("Session Owner: ");
        keyLabel = addAttribute("Annotation Key: ");
        valueLabel = addAttribute("Annotation Value: ");
        ownerLabel = addAttribute("Annotation Owner: ");
        creationDateLabel = addAttribute("Creation Date: ");

        add(attrPanel, BorderLayout.CENTER);

        JButton okButton = new JButton("OK");
        okButton.setActionCommand(CLICKED_OK);
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

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                setVisible(false);
            }
        });
    }
    public void showForAnnotation(OntologyAnnotation annotation) {

    	keyLabel.setText(annotation.getKeyString());
    	valueLabel.setText(annotation.getValueString());
        ownerLabel.setText(annotation.getOwner());
        creationDateLabel.setText(df.format(annotation.getEntity().getCreationDate()));
        
    	try {
    		AnnotationSession session = ModelMgr.getModelMgr().getAnnotationSession(annotation.getSessionId());	
    		sessionLabel.setText(session == null ? "None": session.getName());
    		sessionOwnerLabel.setText(session == null ? "": session.getOwner());
    	}
    	catch (Exception e) {
    		SessionMgr.getSessionMgr().handleException(e);
    	}

        SwingUtilities.updateComponentTreeUI(this);
        setVisible(true);
    }
}
