package org.janelia.it.workstation.gui.framework.tree;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.ConcurrentUtils;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Saves and restores expansion state for a DynamicTree. Asynchronously loads any lazy nodes that were expanded when 
 * the state was stored. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ExpansionState {

    private static final Logger log = LoggerFactory.getLogger(ExpansionState.class);
    
	private final Set<String> expanded = new HashSet<String>();
	private String selected;
	
	private final Set<String> workers = Collections.synchronizedSet(new HashSet<String>());
	boolean startedAllWorkers = false;
	boolean calledSuccess = false;

	public void addExpandedUniqueId(String uniqueId) {
		expanded.add(uniqueId);
	}
	
	public void setSelectedUniqueId(String uniqueId) {
		this.selected = uniqueId;
		// In case you want to select something that was not expanded already...
		expanded.addAll(EntityUtils.getPathFromUniqueId(uniqueId));
	};
	
    public void storeExpansionState(DynamicTree dynamicTree) {
        if (dynamicTree==null) return;
    	expanded.clear();
    	this.selected = dynamicTree.getUniqueId(dynamicTree.getCurrentNode());
    	storeExpansionState(dynamicTree, dynamicTree.getRootNode());
    }
    
	public void storeExpansionState(DynamicTree dynamicTree, DefaultMutableTreeNode node) {
	    if (dynamicTree==null) return;
    	if (dynamicTree.getTree().isExpanded(new TreePath(node.getPath()))) {
    	    String uniqueId = dynamicTree.getUniqueId(node);
            log.debug("storeExpansionState Node@{} ({})",System.identityHashCode(node),uniqueId);
    		expanded.add(uniqueId);
    	}

        for (Enumeration e = node.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
            storeExpansionState(dynamicTree, childNode);
        }
    }

    public void restoreExpansionState(DynamicTree dynamicTree, boolean restoreSelection) {
        if (dynamicTree==null) return;
    	restoreExpansionState(dynamicTree, dynamicTree.getRootNode(), restoreSelection, null);
    }
    
    public void restoreExpansionState(DynamicTree dynamicTree, boolean restoreSelection, Callable<Void> success) {
        if (dynamicTree==null) return;
    	restoreExpansionState(dynamicTree, dynamicTree.getRootNode(), restoreSelection, success);
    	setStartedAllWorkers(true);
    	callSuccessFunction(success);
    }
    
	public void restoreExpansionState(final DynamicTree dynamicTree, final DefaultMutableTreeNode node, 
			final boolean restoreSelection, final Callable<Void> success) {
		
		final String uniqueId = dynamicTree.getUniqueId(node);
		final boolean expand = expanded.contains(uniqueId);
		final boolean select = selected != null && selected.equals(uniqueId);
		
    	if (!expand && !select) return;
    	
    	log.debug("restoreExpansionState Node@{} ({})",System.identityHashCode(node),"expand="+expand+" select="+select);
    	
		if (!dynamicTree.childrenAreLoaded(node)) {
		
		    log.debug("  children are NOT loaded, calling expandNodeWithLazyChildren");
		    
		    workers.add(uniqueId);
			Utils.setWaitingCursor(dynamicTree);
            
			dynamicTree.expandNodeWithLazyChildren(node, new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					Utils.setDefaultCursor(dynamicTree);
					restoreExpansionState(dynamicTree, node, restoreSelection, success);
					workers.remove(uniqueId);
					// The last worker to finish calls the success function
					callSuccessFunction(success);
					return null;
				}
			});
			
			return;
		}
		
    	if (expand) {
    		dynamicTree.expand(node, true);
    	}

    	if (restoreSelection && select) {
    		dynamicTree.navigateToNode(node);
    	}
    	
        for (Enumeration e = node.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
            restoreExpansionState(dynamicTree, childNode, restoreSelection, success);
        }
    }

    private void callSuccessFunction(Callable<Void> success) {
		if (hasStartedAllWorkers() && workers.isEmpty() && !hasCalledSuccess()) {
			try {
			    ConcurrentUtils.invoke(success);
				setCalledSuccess(true);
			}
			catch (Exception e) {
				SessionMgr.getSessionMgr().handleException(e);
			}
		}
    }
    
	public synchronized boolean hasStartedAllWorkers() {
		return startedAllWorkers;
	}

	public synchronized void setStartedAllWorkers(boolean startedAllWorkers) {
		this.startedAllWorkers = startedAllWorkers;
	}

	public boolean hasCalledSuccess() {
		return calledSuccess;
	}

	public void setCalledSuccess(boolean calledSuccess) {
		this.calledSuccess = calledSuccess;
	}
}
