/*
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 6/1/11
 * Time: 4:55 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.Enumeration;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.text.Position;
import javax.swing.tree.*;

import org.janelia.it.FlyWorkstation.gui.util.TreeSearcher;

/**
 * A reusable tree component with toolbar features.
 * 
 * @author saffordt
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DynamicTree extends JPanel {

    protected DefaultMutableTreeNode rootNode;
    protected DefaultTreeModel treeModel;
    protected JTree tree;
    private Toolkit toolkit = Toolkit.getDefaultToolkit();

    public DynamicTree(DefaultTreeCellRenderer cellRenderer, Object userObject) {
        super(new BorderLayout());

        rootNode = new DefaultMutableTreeNode(userObject);
        treeModel = new DefaultTreeModel(rootNode);

        tree = new JTree(treeModel);
        tree.setRowHeight(25);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(cellRenderer);

        DynamicTreeToolbar toolbar = new DynamicTreeToolbar(this);
        add(toolbar, BorderLayout.PAGE_START);
        
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(300,800));
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Returns the underlying JTree.
     * @return
     */
    public JTree getTree() {
        return tree;
    }

    /**
     * Returns the underlying tree model.
     * @return
     */
	public DefaultTreeModel getTreeModel() {
		return treeModel;
	}
	
    /**
     * Returns the root node of the tree.
     * @return
     */
    public DefaultMutableTreeNode getRootNode() {
		return rootNode;
	}

	/**
     * Remove the currently selected node.
     */
    public void removeCurrentNode() {
        TreePath currentSelection = tree.getSelectionPath();
        if (currentSelection != null) {
            DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) (currentSelection.getLastPathComponent());
            MutableTreeNode parent = (MutableTreeNode) (currentNode.getParent());
            if (parent != null) {
                treeModel.removeNodeFromParent(currentNode);
                return;
            }
        }

        // Either there was no selection, or the root was selected.
        toolkit.beep();
    }

    /**
      * Get the currently selected node.
      */
     public DefaultMutableTreeNode getCurrentNode() {
         TreePath currentSelection = tree.getSelectionPath();
         if (currentSelection != null) {
             return (DefaultMutableTreeNode) (currentSelection.getLastPathComponent());
         }
         return null;
     }

     /**
     * Add child to the currently selected node.
     */
    public DefaultMutableTreeNode addObject(Object child) {
        DefaultMutableTreeNode parentNode = null;
        TreePath parentPath = tree.getSelectionPath();

        if (parentPath == null) {
            parentNode = rootNode;
        }
        else {
            parentNode = (DefaultMutableTreeNode) (parentPath.getLastPathComponent());
        }

        return addObject(parentNode, child, true);
    }

    public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent, Object child) {
        return addObject(parent, child, false);
    }

    public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent, Object child, boolean shouldBeVisible) {
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);

        if (parent == null) {
            parent = rootNode;
        }

        // It is key to invoke this on the TreeModel, and NOT DefaultMutableTreeNode
        treeModel.insertNodeInto(childNode, parent, parent.getChildCount());

        // Make sure the user can see the lovely new node.
        if (shouldBeVisible) {
            tree.scrollPathToVisible(new TreePath(childNode.getPath()));
        }
        return childNode;
    }

    /**
     * Expand or collapse all the nodes in the tree.
     * @param expand expand or collapse?
     */
	public void expandAll(boolean expand) {
		TreeNode root = (TreeNode) tree.getModel().getRoot();
		expandAll(new TreePath(root), expand);
	}

	private void expandAll(TreePath parent, boolean expand) {
		
		// Traverse children
		TreeNode node = (TreeNode) parent.getLastPathComponent();
		if (node.getChildCount() >= 0) {
			for (Enumeration e = node.children(); e.hasMoreElements();) {
				TreeNode n = (TreeNode) e.nextElement();
				TreePath path = parent.pathByAddingChild(n);
				expandAll(path, expand);
			}
		}

		// Expansion or collapse must be done bottom-up
		if (expand) {
			tree.expandPath(parent);
		} else {
			tree.collapsePath(parent);
		}
	}

	/**
	 * Iterates through the tree structure and calls treeModel.nodeChanged() on each descendant of the 
	 * given node.
	 * @param currentNode 
	 * @return
	 */
    public DefaultMutableTreeNode refreshDescendants(DefaultMutableTreeNode currentNode) {
        treeModel.nodeChanged(currentNode);
        Enumeration enumeration = currentNode.children();
        while(enumeration.hasMoreElements()) {
            refreshDescendants((DefaultMutableTreeNode)enumeration.nextElement());
        }

        return null;
    }

    /**
     * Move to the next row in the tree. If we are already at the end of the tree, go back to the first row.
     */
    public void navigateToNextRow() {

        int[] selection = tree.getSelectionRows();
        if (selection != null && selection.length > 0) {
            int nextRow = selection[0]+1;
            if (nextRow >= tree.getRowCount()) {
                tree.setSelectionRow(0);
            }
            else {
                tree.setSelectionRow(nextRow);
            }
        }
    }

    /**
     * Select the node with the given user object and scroll to it.
     * @param targetUserObject
     */
    public void navigateToNodeWithObject(Object targetUserObject) {
    	if (targetUserObject == null) tree.setSelectionPath(null);
        DefaultMutableTreeNode node = getNodeForUserObject(targetUserObject, (DefaultMutableTreeNode) treeModel.getRoot());
        navigateToNode(node);
    }

    private DefaultMutableTreeNode getNodeForUserObject(Object targetUserObject, DefaultMutableTreeNode currentNode) {
        if (currentNode.getUserObject().equals(targetUserObject)) {
            return currentNode;
        }

        Enumeration enumeration = currentNode.children();
        while(enumeration.hasMoreElements()) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)enumeration.nextElement();
            DefaultMutableTreeNode foundNode = getNodeForUserObject(targetUserObject, childNode);
            if (foundNode != null) return foundNode;
        }

        return null;
    }
    
    /**
     * Select the given node and scroll to it.
     * @param node
     */
	public void navigateToNode(DefaultMutableTreeNode node) {
		if (node == null) {
			tree.setSelectionPath(null);
		}
		TreePath treePath = new TreePath(node.getPath());
		tree.expandPath(treePath);
		tree.setSelectionPath(treePath);
		tree.scrollPathToVisible(treePath);
	}
    
	/**
	 * Select the node containing the given search string. If bias is null then we search forward starting with the 
	 * current node. If the current node contains the searchString then we don't move. If the bias is Forward then we
	 * start searching in the node after the selected one. If bias is Backward then we look backwards from the node 
	 * before the selected one. 
	 * @param searchString
	 * @param bias
	 */
    public void navigateToNodeStartingWith(String searchString, Position.Bias bias) {
    	
    	TreePath selectionPath = tree.getSelectionPath();
    	DefaultMutableTreeNode startingNode = (selectionPath == null) ? null : (DefaultMutableTreeNode)selectionPath.getLastPathComponent();
    	TreeSearcher searcher = new TreeSearcher(treeModel, searchString, startingNode, bias);
    	DefaultMutableTreeNode node = searcher.find();
    	if (node != null) {
    		navigateToNode(node);
    	}
    }
}
