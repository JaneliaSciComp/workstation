package org.janelia.it.workstation.gui.browser.nodes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.text.Position.Bias;

import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Searches a tree model forward or backward to find nodes matching some search string. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CustomTreeFind {

    private static final Logger log = LoggerFactory.getLogger(CustomTreeFind.class);
    
    // Search parameters
    private final CustomTreeView dynamicTree;
    private final String searchString;
    private final Node startingNode;
    private final Bias bias;

    // Internal search state
    private boolean skipStartingNode = false;
    private Node firstMatch;
    private boolean looking = false;
    private boolean hasRun = false;
        
    /**
     * Create a new searcher. Searchers are not reusable, you must create a new one for each search.
     * @param customTreeView the tree to be searched
     * @param searchString the string to search for
     * @param startingNode the node at which to start searching
     * @param bias Search backwards or forwards from the startingNode? Defaults to forwards.
     * @param skipStartingNode start matching with the starting node, or skip it?
     */
    public CustomTreeFind(CustomTreeView customTreeView, String searchString, Node startingNode, 
    		Bias bias, boolean skipStartingNode) {
        this.dynamicTree = customTreeView;
        this.searchString = searchString.toUpperCase();
        this.startingNode = startingNode;
        this.bias = bias == null ? Bias.Forward : bias;
        this.skipStartingNode = skipStartingNode;
    }

    /**
     * Execute the search and return the first matching node found. 
     * @return
     */
    protected Node find() {
        if (hasRun) throw new IllegalStateException("Cannot reuse TreeSearcher once it has been run.");
        hasRun = true;
        Node foundNode = find((Node) dynamicTree.getRootNode());
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
    protected Node find(Node currNode) {
    	    	
    	// Searching background, so check the current node first (it comes before its children)
        if (bias == Bias.Forward) {
            Node found = checkCurrent(currNode);
            if (found != null) return found;
        }

        // Now we can retrieve the children
        Children children = currNode.getChildren();
        
        log.info("testing "+children+", "+currNode.getDisplayName());
//        List<Node> childNodes = Arrays.asList(children.getNodes());
        List<Node> childNodes = children.snapshot();
        
        // If we're searching backward then we have to walk the children in reverse order
        if (bias == Bias.Backward) Collections.reverse(childNodes);
        
        for (Node child : childNodes) {
            Node found = find(child);
            if (found != null) return found;
        }

        // Searching background, so check the current node last (it comes before its children)
        if (bias == Bias.Backward) {
            Node found = checkCurrent(currNode);
            if (found != null) return found;
        }

        return null;
    }

    /**
     * 
     * @param currNode
     * @return
     */
    protected Node checkCurrent(Node currNode) {

    	// Begin actually looking only once we get to the starting node
        if (currNode.equals(startingNode)) {
            looking = true;
        }

        String name = currNode.getDisplayName();
        
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