package org.janelia.it.workstation.gui.browser.gui.tree;

import com.google.common.base.Predicates;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.text.Position.Bias;

import org.openide.nodes.Children;
import org.openide.nodes.Node;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withModifier;
import static org.reflections.ReflectionUtils.withName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Searches a custom tree forward or backward to find nodes matching some search string. 
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
        this.startingNode = startingNode==null?customTreeView.getRootNode():startingNode;
        this.bias = bias == null ? Bias.Forward : bias;
        this.skipStartingNode = skipStartingNode;
    }

    /**
     * Execute the search and return the first matching node found. 
     * @return
     */
    public Node find() {
        if (hasRun) throw new IllegalStateException("Cannot reuse TreeSearcher once it has been run.");
        hasRun = true;
        Node foundNode = find((Node) dynamicTree.getRootNode());
        if (foundNode != null) {
            return foundNode;
        }
        return firstMatch;
    }

    private Node find(Node currNode) {
    	    
    	// Searching background, so check the current node first (it comes before its children)
        if (bias == Bias.Forward) {
            Node found = checkCurrent(currNode);
            if (found != null) return found;
        }

        // Now we can retrieve the children
        Children children = currNode.getChildren();
        
        boolean inited = false;
        try {
            // I couldn't find any other way of doing this. NetBeans should 
            // really expose a way to find out if the children have been loaded. 
            Set<Method> isInitMethods = getAllMethods(Children.class, 
                Predicates.and(withModifier(Modifier.PROTECTED), withName("isInitialized")));
            Method isInit = isInitMethods.iterator().next();
            isInit.setAccessible(true);
            inited = (Boolean)isInit.invoke(children);
        }
        catch (Exception e) {
            // We don't alert the user here because if this exception happens
            // it's likely to happen over and over. Just log it and fail the search.
            log.error("Problem checking if the nodes have been loaded",e);
        }
        
        if (inited) {
            // Need to make a copy so that we don't modify the underlying children array
            List<Node> childNodes = new ArrayList<>(Arrays.asList(children.getNodes()));

            // If we're searching backward then we have to walk the children in reverse order
            if (bias == Bias.Backward) {
                Collections.reverse(childNodes);
            }

            for (Node child : childNodes) {
                Node found = find(child);
                if (found != null) return found;
            }
        }

        // Searching background, so check the current node last (it comes before its children)
        if (bias == Bias.Backward) {
            Node found = checkCurrent(currNode);
            if (found != null) return found;
        }

        return null;
    }

    private Node checkCurrent(Node currNode) {

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