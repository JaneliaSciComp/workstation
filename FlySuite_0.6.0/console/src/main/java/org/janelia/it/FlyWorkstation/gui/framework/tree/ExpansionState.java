package org.janelia.it.FlyWorkstation.gui.framework.tree;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;

/**
 * Saves and restores expansion state for a DynamicTree. Asynchronously loads any lazy nodes that were expanded when 
 * the state was stored. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ExpansionState {

	private final Set<String> expanded = new HashSet<String>();
	private String selected;
	
    public void storeExpansionState(DynamicTree dynamicTree) {
    	expanded.clear();
    	this.selected = dynamicTree.getUniqueId(dynamicTree.getCurrentNode());
    	storeExpansionState(dynamicTree, dynamicTree.getRootNode());
    }
    
	public void storeExpansionState(DynamicTree dynamicTree, DefaultMutableTreeNode node) {
    	if (dynamicTree.getTree().isExpanded(new TreePath(node.getPath()))) {
    		expanded.add(dynamicTree.getUniqueId(node));
    	}

        for (Enumeration e = node.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
            storeExpansionState(dynamicTree, childNode);
        }
    }

    public void restoreExpansionState(DynamicTree dynamicTree, boolean restoreSelection) {
    	restoreExpansionState(dynamicTree, dynamicTree.getRootNode(), restoreSelection);
    }
    
	public void restoreExpansionState(final DynamicTree dynamicTree, final DefaultMutableTreeNode node, 
			final boolean restoreSelection) {

		final String uniqueId = dynamicTree.getUniqueId(node);
		final boolean expand = expanded.contains(uniqueId);
		final boolean select = selected != null && selected.equals(uniqueId);
		
    	if (!expand && !select) return;
		
		if (!dynamicTree.childrenAreLoaded(node)) {
		
			SimpleWorker loadingWorker = new LazyTreeNodeLoader(dynamicTree, node, false) {

				protected void doneLoading() {
					restoreExpansionState(dynamicTree, node, restoreSelection);
				}

				@Override
				protected void hadError(Throwable error) {
					SessionMgr.getSessionMgr().handleException(error);
				}
			};

			loadingWorker.execute();
			
			return;
		}
		
    	if (expand) {
    		dynamicTree.expand(node, true);
    	}

    	if (restoreSelection && select) {
    		dynamicTree.navigateToNodeWithUniqueId(uniqueId);
    	}
    	
        for (Enumeration e = node.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
            restoreExpansionState(dynamicTree, childNode, restoreSelection);
        }
    }
}
