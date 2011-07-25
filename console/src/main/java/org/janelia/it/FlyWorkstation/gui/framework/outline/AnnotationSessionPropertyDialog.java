package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.ontology.OntologyElement;

/**
 * A dialog for creating a new annotation session, or editing an existing one. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationSessionPropertyDialog extends JDialog implements ActionListener {

    private static final String CANCEL_COMMAND = "cancel";
    private static final String SESSION_SAVE_COMMAND = "session_save";
	
	private TextField nameValueField;
	private JLabel ownerValueLabel;
    
    private SelectionTreePanel entityTreePanel;
    private SelectionTreePanel categoryTreePanel;
    
	public AnnotationSessionPropertyDialog(final OntologyOutline ontologyOutline) {

        setModalityType(ModalityType.APPLICATION_MODAL);
        setPreferredSize(new Dimension(800, 600));
        getContentPane().setLayout(new BorderLayout());
        
        GridBagConstraints c = new GridBagConstraints();
        
        JPanel attrPanel = new JPanel(new GridBagLayout());
        attrPanel.setBorder(
        		BorderFactory.createCompoundBorder(
        				BorderFactory.createEmptyBorder(10, 10, 0, 10), 
        				BorderFactory.createTitledBorder(
        								BorderFactory.createEtchedBorder(), "Session Properties")));

        
        
        JLabel nameLabel = new JLabel("Name: ");
        nameLabel.setAlignmentX(RIGHT_ALIGNMENT);
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 10, 0);
        attrPanel.add(nameLabel, c);

        nameValueField = new TextField();
        nameValueField.setColumns(40);
        c.gridx = 1;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 10, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        attrPanel.add(nameValueField, c);
        
        JLabel ownerLabel = new JLabel("Owner: ");
        nameLabel.setAlignmentX(RIGHT_ALIGNMENT);
        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(0, 0, 10, 0);
        attrPanel.add(ownerLabel, c);

        ownerValueLabel = new JLabel("");
        c.gridx = 1;
        c.gridy = 1;
        c.insets = new Insets(0, 0, 10, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        attrPanel.add(ownerValueLabel, c);
        
        add(attrPanel, BorderLayout.NORTH);
        
        
        JPanel treesPanel = new JPanel(new GridLayout(1,2));
        
        entityTreePanel = new SelectionTreePanel("Entities to annotation") {
    		public void addClicked() {
    			
    		}
        };
        c.gridx = 0;
        c.gridy = 0;
        treesPanel.add(entityTreePanel);
        
        categoryTreePanel = new SelectionTreePanel("Annotations to complete") {
    		public void addClicked() {

    		    OntologyElementChooser ontologyChooser = new OntologyElementChooser("Choose annotations to complete", ontologyOutline.getCurrentOntology());
    			int returnVal = ontologyChooser.showDialog(AnnotationSessionPropertyDialog.this);
    	        if (returnVal != OntologyElementChooser.CHOOSE_OPTION) return;
    	        for(OntologyElement element : ontologyChooser.getChosenElements()) {
    	        	categoryTreePanel.addItem(element);
    	        }
    	        SwingUtilities.updateComponentTreeUI(this);
    		}
        };
        
        c.gridx = 1;
        c.gridy = 0;
        treesPanel.add(categoryTreePanel);
        
        add(treesPanel, BorderLayout.CENTER);
        
        JButton okButton = new JButton("Save");
        okButton.setActionCommand(SESSION_SAVE_COMMAND);
        okButton.setToolTipText("Save this annotation session");
        okButton.addActionListener(this);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand(CANCEL_COMMAND);
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(this);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(cancelButton);
        add(buttonPane, BorderLayout.SOUTH);

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                setVisible(false);
            }
        });
	}
	
	public void showForNewSession(String name, List<Entity> entities) {

        if (entityTreePanel.getTree() == null) setLocationRelativeTo(ConsoleApp.getMainFrame());
        
        setTitle("New Annotation Session");
        nameValueField.setText(name);
        ownerValueLabel.setText(System.getenv("USER"));

        entityTreePanel.createNewTree();
        entityTreePanel.getTree().setCellRenderer(new EntityTreeCellRenderer());
        
        for(Entity entity : entities) {
        	entityTreePanel.addItem(entity);
        }
        
        categoryTreePanel.createNewTree();
        categoryTreePanel.getTree().setCellRenderer(new OntologyTreeCellRenderer());

        SwingUtilities.updateComponentTreeUI(this);
        setVisible(true);
	}
	
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

		if (CANCEL_COMMAND.equals(cmd)) {
			setVisible(false);
		} 
		else if (SESSION_SAVE_COMMAND.equals(cmd)) {
			// TODO: save the session
			
			setVisible(false);
		} 
    }
}
