package org.janelia.it.FlyWorkstation.gui.framework.outline;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.Enumeration;
/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 6/1/11
 * Time: 4:55 PM
 */
public class DynamicTree extends JPanel {

    protected DefaultMutableTreeNode rootNode;
    protected DefaultTreeModel treeModel;
    protected JTree tree;
    private Toolkit toolkit = Toolkit.getDefaultToolkit();

   
    public DynamicTree(DefaultTreeCellRenderer cellRenderer, Object userObject) {
        super(new GridLayout(1, 0));

        rootNode = new DefaultMutableTreeNode(userObject);
        treeModel = new DefaultTreeModel(rootNode);

        tree = new JTree(treeModel);
        tree.setRowHeight(25);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(cellRenderer);

        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(300,800));
        add(scrollPane);
    }

    public JTree getTree() {
        return tree;
    }

    public DefaultMutableTreeNode getRootNode() {
		return rootNode;
	}

	public DefaultTreeModel getTreeModel() {
		return treeModel;
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

    public void removeRootChildren() {
        try {
            TreePath currentSelection = tree.getSelectionPath();
            if (currentSelection != null) {
                DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) (currentSelection.getPathComponent(0));
                Enumeration enumeration = rootNode.children();
                while(enumeration.hasMoreElements()) {
                    DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)enumeration.nextElement();
                    MutableTreeNode parent = (MutableTreeNode) (currentNode.getParent());
                    if (parent != null) {
                        treeModel.removeNodeFromParent(currentNode);
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void expandAll() {
        // expand to the last leaf from the root
        int row = 0;
        while (row < tree.getRowCount()) {
            tree.expandRow(row);
            row++;
        }
    }

    public DefaultMutableTreeNode refreshDescendants(DefaultMutableTreeNode currentNode) {
        treeModel.nodeChanged(currentNode);
        Enumeration enumeration = currentNode.children();
        while(enumeration.hasMoreElements()) {
            refreshDescendants((DefaultMutableTreeNode)enumeration.nextElement());
        }

        return null;
    }

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

    public void navigateToNode(Object targetUserObject) {
        DefaultMutableTreeNode node = getNodeForUserObject(targetUserObject, (DefaultMutableTreeNode) treeModel.getRoot());
        if (node == null) return;
        
        TreePath treePath = new TreePath(node.getPath());
        tree.expandPath(treePath);
        tree.setSelectionPath(treePath);
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
}
