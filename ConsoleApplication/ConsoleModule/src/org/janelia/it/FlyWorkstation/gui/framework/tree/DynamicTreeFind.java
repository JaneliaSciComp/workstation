package org.janelia.it.FlyWorkstation.gui.framework.tree;

import java.util.Collections;
import java.util.List;

import javax.swing.text.Position.Bias;
import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Searches a tree model forward or backward to find nodes matching some search string. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DynamicTreeFind {

    private static final Logger log = LoggerFactory.getLogger(DynamicTreeFind.class);
    
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
        
    /**
     * Create a new searcher. Searchers are not reusable, you must create a new one for each search.
     * @param dynamicTree the tree to be searched
     * @param searchString the string to search for
     * @param startingNode the node at which to start searching
     * @param bias Search backwards or forwards from the startingNode? Defaults to forwards.
     * @param skipStartingNode start matching with the starting node, or skip it?
     */
    public DynamicTreeFind(DynamicTree dynamicTree, String searchString, DefaultMutableTreeNode startingNode, 
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
    protected DefaultMutableTreeNode find() {
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
    protected DefaultMutableTreeNode find(DefaultMutableTreeNode currNode) {
    	    	
    	// Searching background, so check the current node first (it comes before its children)
        if (bias == Bias.Forward) {
            DefaultMutableTreeNode found = checkCurrent(currNode);
            if (found != null) return found;
        }

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

        if (currNode instanceof LazyTreeNode) return null;
        if (currNode.getUserObject()==null) return null;
        
        // TODO: this is a hack, since this package shouldn't know about the domain objects. Should refactor later.
        EntityData ed = (EntityData)currNode.getUserObject();
        Entity child = ed.getChildEntity();
        
        if (child==null) return null;
        String name = child.getName();
        
        if (name.toUpperCase().contains(searchString)) {
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
}