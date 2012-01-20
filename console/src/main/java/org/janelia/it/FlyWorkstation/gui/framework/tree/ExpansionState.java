package org.janelia.it.FlyWorkstation.gui.framework.tree;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * Saves and restores expansion state for a DynamicTree.
 * TODO: this should be aware of lazy trees and use the LazyTreeNodeLoader to load nodes as necessary
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ExpansionState {

	private final Set<String> expanded = new HashSet<String>();
	private String selected;
	
    public void storeExpansionState(DynamicTree dynamicTree) {
    	expanded.clear();
    	this.selected = getPath(dynamicTree.getCurrentNode());
    	storeExpansionState(dynamicTree, dynamicTree.getRootNode());
    }
    
	public void storeExpansionState(DynamicTree dynamicTree, DefaultMutableTreeNode node) {
    	if (dynamicTree.getTree().isExpanded(new TreePath(node.getPath()))) {
    		expanded.add(getPath(node));
    	}

        for (Enumeration e = node.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
            storeExpansionState(dynamicTree, childNode);
        }
    }

    public void restoreExpansionState(DynamicTree dynamicTree) {
    	restoreExpansionState(dynamicTree, dynamicTree.getRootNode());
    }
    
	public void restoreExpansionState(DynamicTree dynamicTree, DefaultMutableTreeNode node) {
		
		String path = getPath(node);
		
    	if (expanded.contains(path)) {
    		dynamicTree.expand(node, true);
    	}

    	if (selected != null && selected.equals(path)) {
    		dynamicTree.navigateToNode(node);
    	}
    	
        for (Enumeration e = node.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
            restoreExpansionState(dynamicTree, childNode);
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
