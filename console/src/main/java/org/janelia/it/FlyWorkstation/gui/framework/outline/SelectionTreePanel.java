package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.janelia.it.FlyWorkstation.gui.framework.tree.DynamicTree;

/**
 * A panel containing a tree which supports adding and removing items at its top level. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SelectionTreePanel extends JPanel implements ActionListener {

    private static final String ADD_COMMAND = "add";
    private static final String REMOVE_COMMAND = "remove";
	
    private DynamicTree tree;		
	private JPanel treePanel;

    public SelectionTreePanel(String title) {
        super(new BorderLayout());
        
        setBorder(BorderFactory.createCompoundBorder(
        				BorderFactory.createEmptyBorder(0, 10, 0, 10), 
        				BorderFactory.createTitledBorder(
        						BorderFactory.createCompoundBorder(
	    	        				BorderFactory.createEmptyBorder(),
	    	        				BorderFactory.createEmptyBorder(10, 10, 0, 10)), title)));
        
        treePanel = new JPanel(new BorderLayout());
        
        add(treePanel, BorderLayout.CENTER);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBackground(new Color(0.8f, 0.8f, 0.8f));
        
        buttonPane.add(Box.createHorizontalGlue());
        
        JButton addButton = new JButton("Add");
        addButton.setActionCommand(ADD_COMMAND);
        addButton.addActionListener(this);
        buttonPane.add(addButton);

        JButton removeButton = new JButton("Remove");
        removeButton.setActionCommand(REMOVE_COMMAND);
        removeButton.addActionListener(this);
        buttonPane.add(removeButton);
        
        add(buttonPane, BorderLayout.SOUTH);
        
    }
    
    public DynamicTree getDynamicTree() {
		return tree;
	}
    
    /**
     * Add an item at the top level of the tree.
     * @param object
     */
    public void addItem(Object object) {
    	DefaultMutableTreeNode node = getDynamicTree().addObject(getDynamicTree().getRootNode(), object);
    	TreePath path = new TreePath(node.getPath());
    	getDynamicTree().getTree().setSelectionPath(path);
    	getDynamicTree().getTree().scrollPathToVisible(path);
    }
    
    /**
     * Return the currently selected items.
     * @return
     */
    public List<Object> getItems() {
    	
    	List<Object> items = new ArrayList<Object>();
    	DefaultMutableTreeNode rootNode = getDynamicTree().getRootNode();
    	
		for (Enumeration e = rootNode.children(); e.hasMoreElements();) {
			DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
			items.add(childNode.getUserObject());
		}
		
		return items;
    }

	public void createNewTree() {
    	tree = new DynamicTree("ROOT", false, false);
        tree.getTree().setRootVisible(false);
        tree.getTree().getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        treePanel.removeAll();
        treePanel.add(tree);
    }
	
	/**
	 * Override this method to do something when the user clicks the "Add" button. 
	 */
	public void addClicked() {
	}
	
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

		if (REMOVE_COMMAND.equals(command)) {
			TreePath[] paths = tree.getTree().getSelectionPaths();
			if (paths == null) return;
			for(TreePath path : paths) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
				tree.removeNode(node);
			}
            SwingUtilities.updateComponentTreeUI(this);
		}
		else if (ADD_COMMAND.equals(command)) {
			addClicked();
		}
    }
}