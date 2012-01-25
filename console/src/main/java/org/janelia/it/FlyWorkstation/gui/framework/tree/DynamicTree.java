/*
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 6/1/11
 * Time: 4:55 PM
 */
package org.janelia.it.FlyWorkstation.gui.framework.tree;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CancellationException;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.text.Position;
import javax.swing.tree.*;

import org.janelia.it.FlyWorkstation.shared.util.Utils;

/**
 * A reusable tree component with toolbar features and an extended API.
 *
 * @author saffordt
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DynamicTree extends JPanel {

    protected final JTree tree;
    protected boolean lazyLoading;
    protected DynamicTreeToolbar toolbar;
    protected LazyTreeSearcher searcher;
    
    public DynamicTree(Object userObject) {
        this(userObject, true, false);
    }

    public DynamicTree(Object userObject, boolean showToolbar, boolean lazyLoading) {
        super(new BorderLayout());

        this.lazyLoading = lazyLoading;

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(userObject);
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);

        tree = new JTree(treeModel);
        tree.setRowHeight(25);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setShowsRootHandles(true);
        tree.setLargeModel(true);

        tree.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {

                int row = tree.getRowForLocation(e.getX(), e.getY());
                if (e.isPopupTrigger()) {
                    tree.setSelectionRow(row);
                    showPopupMenu(e);
                    return;
                }
                if (row >= 0) {
                    // This masking is to make sure that the right button is being double clicked, not left and then right or right and then left
                    if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1 && (e.getModifiersEx() | InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
                        nodeDoubleClicked(e);
                    }
                    else if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1) {
                        nodeClicked(e);
                    }
                }
            }

            public void mousePressed(MouseEvent e) {
                // We have to also listen for mousePressed because OSX generates the popup trigger here
                // instead of mouseReleased like any sane OS.
                int row = tree.getRowForLocation(e.getX(), e.getY());
                if (e.isPopupTrigger()) {
                    tree.setSelectionRow(row);
                    showPopupMenu(e);
                    return;
                }
                if (row >= 0) {
                    nodePressed(e);
                }
            }
        });

        tree.addTreeWillExpandListener(new TreeWillExpandListener() {

            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                if (isLazyLoading()) {
                    final DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                    if (!childrenAreLoaded(node)) {
                        expandNodeWithLazyChildren(node);
                    }
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                // We don't care
            }
        });

        if (showToolbar) {
            toolbar = new DynamicTreeToolbar(this);
            add(toolbar, BorderLayout.PAGE_START);
        }

        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(300, 800));
        add(scrollPane, BorderLayout.CENTER);
    }

    public boolean isLazyLoading() {
        return lazyLoading;
    }

    protected void setLazyLoading(boolean lazyLoading) {
        this.lazyLoading = lazyLoading;
    }

    /**
     * Returns true if the children of the given node have been loaded. Always returns true if the tree is not lazy.
     *
     * @param node
     * @return
     */
    public boolean childrenAreLoaded(DefaultMutableTreeNode node) {

        if (!isLazyLoading()) return true;

        boolean allLoaded = true;
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
            if (childNode instanceof LazyTreeNode) {
                allLoaded = false;
                break;
            }
        }
        return allLoaded;
    }
    
    /**
     * Returns true if all of the descendants of the given node have been loaded. Always returns true if the tree is not lazy.
     *
     * @param node
     * @return
     */
    public boolean descendantsAreLoaded(DefaultMutableTreeNode node) {

        if (!isLazyLoading()) return true;

        boolean allLoaded = true;
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
            if (childNode instanceof LazyTreeNode || !descendantsAreLoaded(childNode)) {
                allLoaded = false;
                break;
            }
        }
        return allLoaded;
    }

    /**
     * Remove all the children of the given node.
     *
     * @param node
     */
    public void removeChildren(DefaultMutableTreeNode node) {

        int c = node.getChildCount();
        if (c==0) return;
        
        int[] childIndices = new int[c];
        Object[] removedChildren = new Object[c];

        for (int i = 0; i < c; i++) {
            childIndices[i] = i;
            removedChildren[i] = node.getChildAt(i);
        }

        node.removeAllChildren();

        getTreeModel().nodesWereRemoved(node, childIndices, removedChildren);
    }

    /**
     * Override this to load the necessary data for the given node so that its children can be expanded with
     * replaceLazyChildNodes.
     * Call this in a worker thread.
     *
     * @param node
     * @param recurse
     */
    public void loadLazyNodeData(DefaultMutableTreeNode node, boolean recurse) throws Exception {
        throw new UnsupportedOperationException("This tree does not support lazy loading");
    }

    /**
     * Override this to replace the current child nodes of the given node with new nodes created from its data.
     * Call this in the EDT.
     *
     * @param node
     * @param recurse
     */
    protected void recreateChildNodes(DefaultMutableTreeNode node) {
        throw new UnsupportedOperationException("This tree does not support child recreation");
    }

    /**
     * Remove a node and possibly all its ancestors from the tree.
     */
    public void removeNode(DefaultMutableTreeNode node, boolean recurse) {
		
        if (recurse) {
            for (Enumeration e = node.children(); e.hasMoreElements(); ) {
                DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
                removeNode(childNode, true);
            }
        }

		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		model.removeNodeFromParent(node);
    }


    /**
     * Remove a node from the tree.
     */
    public void removeNode(DefaultMutableTreeNode node) {
        removeNode(node, false);
    }
    
    /**
     * Override this method to do any necessary lazy loading of children. Do everything in a separate worker thread.
     *
     * @param node
     */
    public void expandNodeWithLazyChildren(DefaultMutableTreeNode node) {
    }

    /**
     * Override this method to show a popup menu when the user right clicks a node in the tree.
     *
     * @param e
     */
    protected void showPopupMenu(MouseEvent e) {
    }

    /**
     * Override this method to do something when the user left clicks a node.
     *
     * @param e
     */
    protected void nodeClicked(MouseEvent e) {
    }

    /**
     * Override this method to do something when the user presses down on a node.
     *
     * @param e
     */
    protected void nodePressed(MouseEvent e) {
    }

    /**
     * Override this method to do something when the user double clicks a node.
     *
     * @param e
     */
    protected void nodeDoubleClicked(MouseEvent e) {
    }

    /**
     * Override this method to provide a unique id for every node, which is required for some DynamicTree consumers 
     * such as ExpansionState. The default implementation throws UnsupportedOperationException.
     * @param node
     * @return
     */
    public String getUniqueId(DefaultMutableTreeNode node) {
    	throw new UnsupportedOperationException();
    }
    
    /**
     * Set the cell renderer on the underlying JTree.
     *
     * @param cellRenderer
     */
    public void setCellRenderer(TreeCellRenderer cellRenderer) {
        tree.setCellRenderer(cellRenderer);
    }

    /**
     * Returns the underlying JTree.
     *
     * @return
     */
    public JTree getTree() {
        return tree;
    }

    /**
     * Returns the underlying tree model.
     *
     * @return
     */
    public DefaultTreeModel getTreeModel() {
        return (DefaultTreeModel) tree.getModel();
    }

    /**
     * Returns the root node of the tree.
     *
     * @return
     */
    public DefaultMutableTreeNode getRootNode() {
        return (DefaultMutableTreeNode) getTreeModel().getRoot();
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

    public void setCurrentNode(DefaultMutableTreeNode currentNode) {
        tree.setSelectionPath(new TreePath(currentNode.getPath()));
    }

    public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parentNode, Object child) {
        return addObject(parentNode, new DefaultMutableTreeNode(child));
    }
    
    public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parentNode, Object child, int index) {
        return addObject(parentNode, new DefaultMutableTreeNode(child), index);
    }

    public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parentNode, DefaultMutableTreeNode childNode) {
    	return addObject(parentNode, childNode, parentNode.getChildCount());
    }
    
    public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parentNode, DefaultMutableTreeNode childNode, int index) {

        if (parentNode == null) {
            parentNode = getRootNode();
        }

        // It is key to invoke this on the TreeModel, and NOT DefaultMutableTreeNode
        getTreeModel().insertNodeInto(childNode, parentNode, index);

        return childNode;
    }

    /**
     * Returns the full path to the current node
     * @param node
     * @return
     */
    public String getStringPath(DefaultMutableTreeNode node) {
    	StringBuffer sb = new StringBuffer();
    	TreeNode curr = node;
    	while(curr != null) {
    		if (node != curr) sb.insert(0, "/");
    		sb.insert(0, curr.toString());
    		curr = curr.getParent();
    	}
    	return sb.toString();
    }
    
    /**
     * Expand or collapse the given node.
     *
     * @param node   the node to expand or collapse
     * @param expand expand or collapse?
     */
    public void expand(DefaultMutableTreeNode node, boolean expand) {
    	TreePath path = new TreePath(node.getPath());
        if (expand) {
        	if (!tree.isExpanded(path)) {
	        	tree.expandPath(path);
        	}
        }
        else {
        	if (tree.isExpanded(path)) {
	            tree.collapsePath(path);
        	}
        }
    }

    /**
     * Expand or collapse all the nodes in the tree.
     *
     * @param expand expand or collapse?
     */
    public void expandAll(boolean expand) {
        expandAll(getRootNode(), expand);
    }

    /**
     * Expand or collapse all the nodes in the given subtree.
     *
     * @param expand expand or collapse?
     */
    public void expandAll(DefaultMutableTreeNode node, boolean expand) {
    	expandAll(new TreePath(node.getPath()), expand);
    }
    
    protected void expandAll(TreePath path, boolean expand) {
    	
        // Traverse children
        TreeNode node = (TreeNode) path.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements(); ) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath childPath = path.pathByAddingChild(n);
                expandAll(childPath, expand);
            }
        }

        // Expansion or collapse must be done bottom-up
        if (expand) {
            tree.expandPath(path);
        }
        else if (tree.isExpanded(path)) {
            tree.collapsePath(path);
        }
    }

    /**
     * Iterates through the tree structure and calls treeModel.nodeChanged() on each descendant of the
     * given node.
     *
     * @param node
     * @return
     */
    public DefaultMutableTreeNode refreshDescendants(DefaultMutableTreeNode node) {
        getTreeModel().nodeChanged(node);
        Enumeration enumeration = node.children();
        while (enumeration.hasMoreElements()) {
            refreshDescendants((DefaultMutableTreeNode) enumeration.nextElement());
        }

        return null;
    }

    /**
     * Move to the next row in the tree. If we are already at the end of the tree, go back to the first row.
     */
    public void navigateToNextRow() {

        int[] selection = tree.getSelectionRows();
        if (selection != null && selection.length > 0) {
            int nextRow = selection[0] + 1;
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
     *
     * @param targetUserObject
     */
    public void navigateToNodeWithObject(Object targetUserObject) {
        if (targetUserObject == null) tree.setSelectionPath(null);
        DefaultMutableTreeNode node = getNodeForUserObject(targetUserObject, getRootNode());
        navigateToNode(node);
    }

    public DefaultMutableTreeNode getNodeForUserObject(Object targetUserObject) {
    	return getNodeForUserObject(targetUserObject, getRootNode());
    }
    
    public DefaultMutableTreeNode getNodeForUserObject(Object targetUserObject, DefaultMutableTreeNode currentNode) {
        if (currentNode.getUserObject() != null && currentNode.getUserObject().equals(targetUserObject)) {
            return currentNode;
        }

        Enumeration enumeration = currentNode.children();
        while (enumeration.hasMoreElements()) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) enumeration.nextElement();
            DefaultMutableTreeNode foundNode = getNodeForUserObject(targetUserObject, childNode);
            if (foundNode != null) return foundNode;
        }

        return null;
    }

    /**
     * Select the given node and scroll to it.
     *
     * @param node
     */
    public void navigateToNode(DefaultMutableTreeNode node) {
        tree.setSelectionPath(null);
        if (node == null) return;
        TreePath treePath = new TreePath(node.getPath());
        tree.setSelectionPath(treePath);
        tree.scrollPathToVisible(treePath);
    }

    /**
     * Select the given nodes and scroll to ensure the first one is displayed.
     *
     * @param node
     */
    public void selectAndShowNodes(List<DefaultMutableTreeNode> nodes) {
        tree.setSelectionPath(null);
        boolean scrolled = false;
        for (DefaultMutableTreeNode node : nodes) {
            TreePath treePath = new TreePath(node.getPath());
            tree.addSelectionPath(treePath);
            if (!scrolled) {
                tree.scrollPathToVisible(treePath);
                scrolled = true;
            }
        }
    }

    /**
     * Select the node containing the given search string. If bias is null then we search forward starting with the
     * current node. If the current node contains the searchString then we don't move. If the bias is Forward then we
     * start searching in the node after the selected one. If bias is Backward then we look backwards from the node
     * before the selected one.
     *
     * @param searchString
     * @param bias
     */
    public void navigateToNodeStartingWith(String searchString, Position.Bias bias, boolean skipStartingNode) {
		
        TreePath selectionPath = tree.getSelectionPath();
        DefaultMutableTreeNode startingNode = (selectionPath == null) ? null : (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        
        if (startingNode == null) {
        	// Nothing is selected, so start with the root, but don't skip it
        	startingNode = getRootNode();
        	skipStartingNode = false;
        }
        
        if (searcher != null && !searcher.isDone()) {
        	searcher.cancel(true);
        	// Wait for it to actually finish, since we don't want to have multiple threads modifying the tree 
        	try {
        		searcher.get();
        	} catch (CancellationException e) {
        		// Expected
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        }
        
        searcher = new LazyTreeSearcher(this, searchString, startingNode, bias, skipStartingNode) {

			@Override
        	protected void foundNode(DefaultMutableTreeNode matchingNode) {
        		if (isCancelled()) return;
	            navigateToNode(matchingNode);
	            setNotSearching();
			}

			@Override
        	protected void noMatches() {
        		if (isCancelled()) return;
        		setNotSearching();
        	}
        	
			@Override
			protected void hadError(Throwable error) {
				error.printStackTrace();
				if (isCancelled()) return;
				JOptionPane.showMessageDialog(DynamicTree.this, "Error searching tree", "Error", JOptionPane.ERROR_MESSAGE);
				setNotSearching();
			}
        	
        };
        
        setSearching();
        searcher.execute();
    }
    
    private void setSearching() {
        if (!isLazyLoading()) return;
    	Utils.setWaitingCursor(DynamicTree.this);
        toolbar.setSpinning(true);
    }
    
    private void setNotSearching() {
    	if (!isLazyLoading()) return;
    	Utils.setDefaultCursor(DynamicTree.this);
		toolbar.setSpinning(false);
    }

    /**
     * Override this method to provide refresh behavior. The default implementation does nothing. 
     */
	protected void refresh() {
	}
    
}


