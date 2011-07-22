/*
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 6/1/11
 * Time: 4:54 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.OntologyRoot;


/**
 * An ontology term chooser.
 * 
 * @author saffordt
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyElementChooserDialog extends JDialog implements ActionListener {
	
    private static final String CHOOSE_COMMAND = "choose";
    private static final String CANCEL_COMMAND = "cancel";
    
    private final JPanel treesPanel;
    private DynamicTree selectedTree;
    
    public OntologyElementChooserDialog() {

        setModalityType(ModalityType.APPLICATION_MODAL);
        setPreferredSize(new Dimension(600, 800));
        getContentPane().setLayout(new BorderLayout());
        setLocationRelativeTo(ConsoleApp.getMainFrame());
        
        add(new JLabel("Choose one or more ontology elements"), BorderLayout.NORTH);

        treesPanel = new JPanel(new BorderLayout());
        add(treesPanel, BorderLayout.CENTER);

        JButton okButton = new JButton("Choose");
        okButton.setActionCommand(CHOOSE_COMMAND);
        okButton.setToolTipText("Choose the selected elements");
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
        
	}
    
    public void showForOntology(OntologyRoot root) {
    	initializeTree(root);
    	setVisible(true);
    }
    
    public void initializeTree(OntologyRoot root) {
    	
        treesPanel.removeAll();

        if (root == null) return;

        // Create a new tree and add all the nodes to it
		
		createNewTree(root);
		addNodes(null, root);
        
        // Replace the tree in the panel

        treesPanel.removeAll();
        treesPanel.add(selectedTree);

        // Prepare for display and update the UI
        
        selectedTree.expandAll(true);
        SwingUtilities.updateComponentTreeUI(this);
    }
    
    private void createNewTree(OntologyElement root) {
    	
    	selectedTree = new DynamicTree(root, false, false) {
            
            protected void nodeDoubleClicked(MouseEvent e) {
            	chooseCurrentNodes();
            }
        };
        
        // Allow multiple selection
        selectedTree.getTree().getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        
        // Replace the cell renderer
        selectedTree.setCellRenderer(new OntologyTreeCellRenderer());
        
    }
    
    public void chooseCurrentNodes() {
    	
		setVisible(false);
    }

    
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

		if (CANCEL_COMMAND.equals(cmd)) {
			setVisible(false);
		} 
		else if (CHOOSE_COMMAND.equals(cmd)) {
			chooseCurrentNodes();
		}
		
    }
    
    private void addNodes(DefaultMutableTreeNode parentNode, OntologyElement element) {

    	// Add the node to the tree
        DefaultMutableTreeNode newNode;
        if (parentNode != null) {
            newNode = selectedTree.addObject(parentNode, element);
        }
        else {
            // If the parent node is null, then the node is already in the tree as the root
            newNode = selectedTree.rootNode;
        }
    	
    	// Add the node's children. 
    	// They are available because the root was loaded with the eager-loading getOntologyTree() method. 
        for(OntologyElement child : element.getChildren()) {
        	addNodes(newNode, child);
        }
    }
}
