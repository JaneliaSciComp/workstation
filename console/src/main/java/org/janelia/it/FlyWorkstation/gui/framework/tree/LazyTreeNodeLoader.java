package org.janelia.it.FlyWorkstation.gui.framework.tree;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;

/**
 * A worker thread which loads the children or ancestors for a node in a DynamicTree. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LazyTreeNodeLoader extends SimpleWorker {

	private final Set<String> expanded = new HashSet<String>();
    private final DynamicTree dynamicTree;
    private final DefaultMutableTreeNode node;
    private final boolean recurse;

    public LazyTreeNodeLoader(DynamicTree dynamicTree, DefaultMutableTreeNode node, boolean recurse) {
        this.dynamicTree = dynamicTree;
        this.node = node;
        this.recurse = recurse;
        storeExpansionState(node);
    }

    /**
     * This is an alternate way of invoking the loader without spawning a new thread. This method should be used instead
     * of execute if the caller is already running in a worker thread. The doneLoading() callback will still get called.
     * @throws Exception
     */
    public void loadSynchronously() throws Exception {
        dynamicTree.loadLazyNodeData(node, recurse);
        dynamicTree.recreateChildNodes(node, recurse);
        restoreExpansionState(node);
        doneLoading();
    }
    
    @Override
    protected void doStuff() throws Exception {
        dynamicTree.loadLazyNodeData(node, recurse);
    }

    @Override
    protected void hadSuccess() {
        dynamicTree.recreateChildNodes(node, recurse);
        restoreExpansionState(node);
        doneLoading();
    }

    @Override
    protected void hadError(Throwable error) {
        error.printStackTrace();
    }

    /**
     * Override this method to do something once the nodes have been loaded. Called in the EDT.
     */
    protected void doneLoading() {
    }
    
    private void storeExpansionState(DefaultMutableTreeNode node) {
    	if (dynamicTree.getTree().isExpanded(new TreePath(node.getPath()))) {
    		expanded.add(getPath(node));
    	}

        for (Enumeration e = node.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
            storeExpansionState(childNode);
        }
    }
    
    private void restoreExpansionState(DefaultMutableTreeNode node) {
    	if (expanded.contains(getPath(node))) {
    		dynamicTree.expand(node, true);
    	}

        for (Enumeration e = node.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
            restoreExpansionState(childNode);
        }
    }
    
    private String getPath(DefaultMutableTreeNode node) {
    	StringBuffer sb = new StringBuffer();
    	TreeNode curr = node;
    	while(curr != null) {
    		if (node != curr) sb.insert(0, "/");
    		sb.insert(0, curr.toString());
    		curr = curr.getParent();
    	}
    	return sb.toString();
    }
}