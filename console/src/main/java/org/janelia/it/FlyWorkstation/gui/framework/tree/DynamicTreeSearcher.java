package org.janelia.it.FlyWorkstation.gui.framework.tree;

import java.util.Collections;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.text.Position.Bias;
import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.it.FlyWorkstation.gui.framework.outline.EntityTree;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;


/**
 * Searches a tree model forward or backward to find nodes matching some search string. 
 * 
 * Aware of lazy trees, and will load nodes as it is searching. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DynamicTreeSearcher {
	
	private final DynamicTree dynamicTree;
	private final String searchString;
	private final DefaultMutableTreeNode startingNode;
	private final Bias bias;
	
	private boolean skipStartingNode = false;
	private DefaultMutableTreeNode firstMatch;
	private boolean looking = false;
	private boolean hasRun = false;
	
	/**
	 * Create a new searcher. Searchers are not reusable, you must create a new one for each search. 
	 * @param treeModel
	 * @param searchString
	 * @param startingNode
	 * @param bias
	 */
	public DynamicTreeSearcher(DynamicTree dynamicTree, String searchString, DefaultMutableTreeNode startingNode, Bias bias) {
		this.dynamicTree = dynamicTree;
		this.searchString = searchString.toUpperCase();
		this.startingNode = startingNode;
		if (bias == null) {
			skipStartingNode = true;
			this.bias = Bias.Forward;
		}
		else {
			this.bias = bias;
		}
	}

	/**
	 * Execute the search and return the first matching node found.
	 * @return
	 */
	public DefaultMutableTreeNode find() {
		if (hasRun) throw new IllegalStateException("Cannot reuse TreeSearcher once it has been run.");
		hasRun = true;
		DefaultMutableTreeNode foundNode = find((DefaultMutableTreeNode)dynamicTree.getRootNode());
		if (foundNode != null) {
			return foundNode;
		}
		return firstMatch;
	}
	
	private DefaultMutableTreeNode find(DefaultMutableTreeNode currNode) {
		
		// TODO: remove this
//
//		if (!dynamicTree.childrenAreLoaded(currNode)) {
//
//			SimpleWorker loadingWorker = new LazyTreeNodeExpansionWorker(dynamicTree, currNode, false) {
//
//				protected void doneExpanding() {
//					
//				}
//
//	        };
//
//	        loadingWorker.execute();
//	        return null;
//		}
		
		if (bias == Bias.Forward) {
			DefaultMutableTreeNode found = checkCurrent(currNode);
			if (found != null) return found;
		}
		
		List<DefaultMutableTreeNode> children = Collections.list(currNode.children());
		if (bias == Bias.Backward) Collections.reverse(children);
		for(DefaultMutableTreeNode child : children) {
			if (!(child instanceof LazyTreeNode)) {
				DefaultMutableTreeNode found = find(child);
				if (found != null) return found;
			}
		}
		
		if (bias == Bias.Backward) {
			DefaultMutableTreeNode found = checkCurrent(currNode);
			if (found != null) return found;
		}
		
		return null;
		
	}
	
	private DefaultMutableTreeNode checkCurrent(DefaultMutableTreeNode currNode) {
		
		if (currNode.equals(startingNode)) {
			looking = true;
		}
		
		if (currNode.getUserObject().toString().toUpperCase().contains(searchString)) {
			if (looking && (skipStartingNode || !currNode.equals(startingNode))) {
				return currNode;
			} 
			else if (firstMatch == null) {
				firstMatch = currNode;
			}
		}
		return null;
	}
}