package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.janelia.it.jacs.model.entity.Entity;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 6/1/11
 * Time: 4:55 PM
 */
class DynamicTree extends JPanel {
    protected EntityMutableTreeNode rootNode;
    protected DefaultTreeModel treeModel;
    protected JTree tree;
    private Toolkit toolkit = Toolkit.getDefaultToolkit();

    public DynamicTree(Entity rootEntity) {
        super(new GridLayout(1, 0));
        String tmpName = (rootEntity.getName()==null)?"Root Node - "+System.currentTimeMillis():rootEntity.getName();
        rootNode = new EntityMutableTreeNode(rootEntity);
        treeModel = new DefaultTreeModel(rootNode);

        tree = new JTree(treeModel);
        tree.setEditable(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setShowsRootHandles(true);

        JScrollPane scrollPane = new JScrollPane(tree);
        add(scrollPane);
    }

    /**
     * Remove the currently selected node.
     */
    public void removeCurrentNode() {
        TreePath currentSelection = tree.getSelectionPath();
        if (currentSelection != null) {
            EntityMutableTreeNode currentNode = (EntityMutableTreeNode) (currentSelection.getLastPathComponent());
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
      * Get the currently selected node name.
      */
     public String getCurrentNodeName() {
         TreePath currentSelection = tree.getSelectionPath();
         if (currentSelection != null) {
             EntityMutableTreeNode currentNode = (EntityMutableTreeNode) (currentSelection.getLastPathComponent());
             return currentNode.toString();
         }
         return null;
     }

    /**
      * Get the currently selected node id.
      */
     public String getCurrentNodeId() {
         TreePath currentSelection = tree.getSelectionPath();
         if (currentSelection != null) {
             EntityMutableTreeNode currentNode = (EntityMutableTreeNode) (currentSelection.getLastPathComponent());
             return ((Entity)currentNode.getUserObject()).getId().toString();
         }
         return null;
     }

     /**
     * Add child to the currently selected node.
     */
    public EntityMutableTreeNode addObject(Object child) {
        EntityMutableTreeNode parentNode = null;
        TreePath parentPath = tree.getSelectionPath();

        if (parentPath == null) {
            parentNode = rootNode;
        }
        else {
            parentNode = (EntityMutableTreeNode) (parentPath.getLastPathComponent());
        }

        return addObject(parentNode, child, true);
    }

    public EntityMutableTreeNode addObject(EntityMutableTreeNode parent, Object child) {
        return addObject(parent, child, false);
    }

    public EntityMutableTreeNode addObject(EntityMutableTreeNode parent, Object child, boolean shouldBeVisible) {
        EntityMutableTreeNode childNode = new EntityMutableTreeNode(child);

        if (parent == null) {
            parent = rootNode;
        }

        // It is key to invoke this on the TreeModel, and NOT EntityMutableTreeNode
        treeModel.insertNodeInto(childNode, parent, parent.getChildCount());

        // Make sure the user can see the lovely new node.
        if (shouldBeVisible) {
            tree.scrollPathToVisible(new TreePath(childNode.getPath()));
        }
        return childNode;
    }

    class MyTreeModelListener implements TreeModelListener {
        public void treeNodesChanged(TreeModelEvent e) {
            EntityMutableTreeNode node;
            node = (EntityMutableTreeNode) (e.getTreePath().getLastPathComponent());

            /*
            * If the event lists children, then the changed node is the child of the
            * node we've already gotten. Otherwise, the changed node and the
            * specified node are the same.
            */

            int index = e.getChildIndices()[0];
            node = (EntityMutableTreeNode) (node.getChildAt(index));

            System.out.println("The user has finished editing the node.");
            System.out.println("New value: " + node.getUserObject());
        }

        public void treeNodesInserted(TreeModelEvent e) {
        }

        public void treeNodesRemoved(TreeModelEvent e) {
        }

        public void treeStructureChanged(TreeModelEvent e) {
        }
    }

}