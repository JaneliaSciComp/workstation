package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.ontology.OntologyRoot;


/**
 * An ontology term chooser that can display an ontology specified by an OntologyRoot and allows the user to select
 * one or more terms for use.
 * 
 * This class follows the pattern set by JFileChooser.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OntologyElementChooser extends JComponent implements ActionListener {

	public static final int ERROR_OPTION = -1;
    public static final int CANCEL_OPTION = 0;
    public static final int CHOOSE_OPTION = 1;
    
    private static final String CHOOSE_COMMAND = "choose";
    private static final String CANCEL_COMMAND = "cancel";
    
    private String title;
    private JPanel treesPanel;
    private DynamicTree selectedTree;
    private JDialog dialog;
    
    private int returnValue = ERROR_OPTION;
    
    private List<OntologyElement> chosenElements = new ArrayList<OntologyElement>();
    
    public OntologyElementChooser(String title, OntologyRoot root) {
    	this.title = title;
    	initializeUI();
    	initializeTree(root);
	}

    public List<OntologyElement> getChosenElements() {
		return chosenElements;
	}
    
	public int showDialog(Component parent) throws HeadlessException {
		
		JDialog dialog = createDialog(parent, title);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				returnValue = CANCEL_OPTION;
			}
		});

		dialog.setVisible(true);
		// Blocks until dialog is no longer visible, and then:
		dialog.removeAll();
		dialog.dispose();
		return returnValue;
	}

    private JDialog createDialog(Component parent, String title) throws HeadlessException {

        if (parent instanceof Frame) {
            dialog = new JDialog((Frame)parent, title, true);	
        } else {
            dialog = new JDialog((JDialog)parent, title, true);
        }
        dialog.setComponentOrientation(this.getComponentOrientation());

        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(this, BorderLayout.CENTER);
 
        if (JDialog.isDefaultLookAndFeelDecorated()) {
            boolean supportsWindowDecorations = 
            UIManager.getLookAndFeel().getSupportsWindowDecorations();
            if (supportsWindowDecorations) {
                dialog.getRootPane().setWindowDecorationStyle(JRootPane.WARNING_DIALOG);
            }
        }
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
    	return dialog;
    }

    private void initializeUI() {

        setPreferredSize(new Dimension(600, 800));
        setLayout(new BorderLayout());

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
    
    private void createNewTree(OntologyRoot root) {
    	
    	selectedTree = new DynamicTree(root, false, false) {
    		@Override
            protected void nodeDoubleClicked(MouseEvent e) {
            	chooseSelection();
            }
        };
        
        // Allow multiple selection
        selectedTree.getTree().getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        
        // Replace the cell renderer
        selectedTree.setCellRenderer(new OntologyTreeCellRenderer());
        
    }

	public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
		if (CANCEL_COMMAND.equals(cmd)) {
			cancelSelection();
		} 
		else if (CHOOSE_COMMAND.equals(cmd)) {
			chooseSelection();
		}
    }
	
    private void chooseSelection() {

    	chosenElements.clear();
    	
		for(TreePath path : selectedTree.getTree().getSelectionPaths()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
			OntologyElement element = (OntologyElement)node.getUserObject();
			chosenElements.add(element);
		}
		
		returnValue = CHOOSE_OPTION;
		dialog.setVisible(false);
    }

    private void cancelSelection() {
    	returnValue = CANCEL_OPTION;
    	dialog.setVisible(false);
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
