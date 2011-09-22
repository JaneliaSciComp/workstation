package org.janelia.it.FlyWorkstation.gui.framework.tree;

import java.util.Collections;
import java.util.List;

import javax.swing.text.Position.Bias;
import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;


/**
 * Searches a tree model forward or backward to find nodes matching some search string. This code runs as a background 
 * thread, allowing it to perform computation asynchronously, such as loading lazy nodes as needed. When creating an 
 * instance of this searchers you should implement foundNode() and probably override hadError() as well. Then simply 
 * call execute() and wait for your callbacks to be called.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class DynamicTreeSearcher extends SimpleWorker {

	// Search parameters
    private final DynamicTree dynamicTree;
    private final String searchString;
    private final DefaultMutableTreeNode startingNode;
    private final Bias bias;

    // Internal search state
    private boolean skipStartingNode = false;
    private DefaultMutableTreeNode firstMatch;
    private boolean looking = false;
    private boolean hasRun = false;
    
    // Final result
    private DefaultMutableTreeNode matchingNode;
    
    /**
     * Create a new searcher. Searchers are not reusable, you must create a new one for each search.
     * @param dynamicTree the tree to be searched
     * @param searchString the string to search for
     * @param startingNode the node at which to start searching
     * @param bias Search backwards or forwards from the startingNode? Defaults to forwards.
     * @param skipStartingNode start matching with the starting node, or skip it?
     */
    public DynamicTreeSearcher(DynamicTree dynamicTree, String searchString, DefaultMutableTreeNode startingNode, 
    		Bias bias, boolean skipStartingNode) {
        this.dynamicTree = dynamicTree;
        this.searchString = searchString.toUpperCase();
        this.startingNode = startingNode;
        this.bias = bias == null ? Bias.Forward : bias;
        this.skipStartingNode = skipStartingNode;
    }

    /**
     * Execute the search and return the first matching node found. 
     * @return
     */
    protected DefaultMutableTreeNode find() throws Exception {
        if (hasRun) throw new IllegalStateException("Cannot reuse TreeSearcher once it has been run.");
        hasRun = true;
        DefaultMutableTreeNode foundNode = find((DefaultMutableTreeNode) dynamicTree.getRootNode());
        if (foundNode != null) {
            return foundNode;
        }
        return firstMatch;
    }

    /**
     * 
     * @param currNode
     * @return
     */
    protected DefaultMutableTreeNode find(DefaultMutableTreeNode currNode) throws Exception {
    	
    	if (isCancelled()) return null;
    	
    	// Searching background, so check the current node first (it comes before its children)
        if (bias == Bias.Forward) {
            DefaultMutableTreeNode found = checkCurrent(currNode);
            if (found != null) return found;
        }

        // Load children if necessary
        if (!dynamicTree.childrenAreLoaded(currNode)) {
        	// We're already running in a background thread, so we can load synchronously
        	LazyTreeNodeLoader loader = new LazyTreeNodeLoader(dynamicTree, currNode, false);
        	loader.loadSynchronously();
        }
        
        if (isCancelled()) return null;
        
        // Now we can retrieve the children
        List<DefaultMutableTreeNode> children = Collections.list(currNode.children());
        
        // If we're searching backward then we have to walk the children in reverse order
        if (bias == Bias.Backward) Collections.reverse(children);
        
        for (DefaultMutableTreeNode child : children) {
            DefaultMutableTreeNode found = find(child);
            if (found != null) return found;
        }

        // Searching background, so check the current node last (it comes before its children)
        if (bias == Bias.Backward) {
            DefaultMutableTreeNode found = checkCurrent(currNode);
            if (found != null) return found;
        }

        return null;
    }

    /**
     * 
     * @param currNode
     * @return
     */
    protected DefaultMutableTreeNode checkCurrent(DefaultMutableTreeNode currNode) {

    	// Begin actually looking only once we get to the starting node
        if (currNode.equals(startingNode)) {
            looking = true;
        }

        if (currNode.getUserObject().toString().toUpperCase().contains(searchString)) {
            // Found a match
        	
            if (looking && (!skipStartingNode || (skipStartingNode && !currNode.equals(startingNode)))) {
            	// This is a good match
                return currNode;
            }
            else if (firstMatch == null) {
            	// We need to use this match if we wrap around without finding anything past the starting node
                firstMatch = currNode;
            }
        }
        return null;
    }

	@Override
	protected void doStuff() throws Exception {
		matchingNode = find();
	}

	@Override
	protected void hadSuccess() {
		if (matchingNode != null) foundNode(matchingNode);
		else noMatches();
	}

	protected abstract void foundNode(DefaultMutableTreeNode matchingNode);

	protected abstract void noMatches();
	
	@Override
	protected abstract void hadError(Throwable error);
    
    
}