package org.janelia.it.FlyWorkstation.gui.framework.tree;

import java.util.Collections;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;
import org.janelia.it.jacs.model.entity.Entity;


/**
 * Searches a large entity tree model by mapping database query results onto the current tree.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class DynamicTreeSearcher extends SimpleWorker {

	// Search parameters
    private final DynamicTree dynamicTree;
    private final String searchString;
    private final DefaultMutableTreeNode startingNode;

    // Internal search state
    private boolean hasRun = false;
    private boolean looking = false;
    private List<Long> firstMatch;
    private List<Long> finalMatch;
    
    /**
     * Create a new searcher. Searchers are not reusable, you must create a new one for each search.
     * @param dynamicTree the tree to be searched
     * @param searchString the string to search for
     * @param startingNode the node at which to start searching
     * @param skipStartingNode start matching with the starting node, or skip it?
     */
    public DynamicTreeSearcher(DynamicTree dynamicTree, String searchString, DefaultMutableTreeNode startingNode) {
        this.dynamicTree = dynamicTree;
        this.searchString = searchString.toUpperCase();
        this.startingNode = startingNode;
    }

    protected List<Long> find() throws Exception {
        if (hasRun) throw new IllegalStateException("Cannot reuse TreeSearcher once it has been run.");
        hasRun = true;
        
        Entity rootEntity = (Entity)dynamicTree.getRootNode().getUserObject();
        
        List<List<Long>> matchingPaths = ModelMgr.getModelMgr().searchTreeForNameStartingWith(rootEntity.getId(), searchString);

        if (matchingPaths.isEmpty()) {
        	System.out.println("No matches");
        	return null;
        }
        
    	for(List<Long> path : matchingPaths) {
        	System.out.println("Potential path: "+Utils.join(path, ","));
    	}
    	
        if (find(dynamicTree.getRootNode(), matchingPaths, 0) && finalMatch!=null) {
			System.out.println("Got a final match "+Utils.join(finalMatch, ","));
            return finalMatch;
        }

        if (firstMatch!=null)
        	System.out.println("Going with first match "+Utils.join(firstMatch, ","));
        else 
        	System.out.println("No match");
        
        return firstMatch;
    }

    protected boolean find(DefaultMutableTreeNode currNode, List<List<Long>> paths, int level) throws Exception {
    	
    	if (isCancelled()) return false;
    	
    	StringBuffer indent = new StringBuffer();
    	for(int i=0; i<level; i++) {
    		indent.append("  ");
    	}
    	
    	if (startingNode.equals(currNode)) {
			System.out.println(indent+">Found starting node, starting to look");
    		looking = true;
    	}
    	
        Entity entity = (Entity)currNode.getUserObject();

    	System.out.println(indent+"Searching "+currNode+" ("+entity.getId()+")");
        
    	for(List<Long> path : paths) {

    		if (!path.isEmpty() && path.get(level).equals(entity.getId())) {
            	System.out.println(indent+">Matching path: "+Utils.join(path, ","));
            	
    			// This path matches so far
    			if (level == path.size()-1) {
    				System.out.println(indent+">Path matches completely");
    				// We've reached the end of the road
    				if (looking) {
        				System.out.println(indent+">final match");
    					finalMatch = path;
        		    	return true;
    				}
    				else if (firstMatch==null) {
    					System.out.println(indent+">first match");
    					firstMatch = path;
    				}
    			}
    			else {
    				System.out.println(indent+">Path matches partially");
    				if (dynamicTree.childrenAreLoaded(currNode)) {
        				// We're on the right track, keep recursing
        		        List<DefaultMutableTreeNode> children = Collections.list(currNode.children());
        		        for (DefaultMutableTreeNode child : children) {        		            
        		            if (find(child, paths, level+1)) {
        		            	return true;
        		            }
        		        }
    				}
    				else {
        				System.out.println(indent+">Lazy nodes encountered");
    					// Blocked by lazy nodes. If we're looking, then this is our path. Otherwise, let's keep going.
        				if (looking) {
            				System.out.println(indent+">final match");
        					finalMatch = path;
            		    	return true;
        				}
        				else if (firstMatch==null) {
        					System.out.println(indent+">first match");
        					firstMatch = path;
        				}
    				}
    			}
    		}
    	}
    	
        return false;
    }

	@Override
	protected void doStuff() throws Exception {
		finalMatch = find();
	}

	@Override
	protected void hadSuccess() {
		if (finalMatch != null) foundPath(finalMatch);
		else noMatches();
	}

	protected abstract void foundPath(List<Long> finalMatch);

	protected abstract void noMatches();
	
	@Override
	protected abstract void hadError(Throwable error);
    
    
}