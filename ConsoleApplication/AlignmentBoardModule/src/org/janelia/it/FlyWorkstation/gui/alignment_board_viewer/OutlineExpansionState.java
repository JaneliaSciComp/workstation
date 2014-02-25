package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.tree.TreePath;

import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.netbeans.swing.outline.Outline;
import org.netbeans.swing.outline.OutlineModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Saves and restores expansion state for an Outline. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class OutlineExpansionState {

    private static final Logger log = LoggerFactory.getLogger(OutlineExpansionState.class);
    
    private Outline outline;
	private final Set<List<Long>> expanded = new HashSet<List<Long>>();
	private Integer selectedRow;
	
	boolean startedAllWorkers = false;
	boolean calledSuccess = false;
	
	public OutlineExpansionState(Outline outline) {
	    this.outline = outline;
	}
	
    public void storeExpansionState() {

        if (outline==null || outline.getOutlineModel()==null) return;
        log.trace("storeExpansionState:");
        
    	expanded.clear();
    	this.selectedRow = outline.getSelectedRow();
    	TreePath rootPath = new TreePath(outline.getOutlineModel().getRoot());

    	for(TreePath treePath : outline.getOutlineModel().getTreePathSupport().getExpandedDescendants(rootPath)) {
    	    List<Long> path = new ArrayList<Long>();
    	    for(Object obj : treePath.getPath()) {
    	        EntityWrapper wrapper = (EntityWrapper)obj;
    	        path.add(wrapper.getId());
    	    }
    	    log.trace("  expanded: "+path);
    	    expanded.add(path);
    	}
        
    }
    
    public void restoreExpansionState(boolean restoreSelection) {
        
        if (outline==null) return;
        
        log.trace("restoreExpansionState:");
        TreePath rootPath = new TreePath(outline.getOutlineModel().getRoot());
    	restoreExpansionState(rootPath, expanded, "");
    	
    	if (restoreSelection && selectedRow!=null) {
    	    outline.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
    	}
    }
    
    private void restoreExpansionState(TreePath treePath, Set<List<Long>> expandedIds, String indent) {

        OutlineModel outlineModel = outline.getOutlineModel();
        log.trace(indent+"restoreExpansionState: "+treePath);
        
        EntityWrapper nodeObject = (EntityWrapper)treePath.getLastPathComponent();
        
        Set<List<Long>> relativeExpandedIds = new HashSet<List<Long>>();
        for(List<Long> expandedPath : expandedIds) {
            if (expandedPath.get(0).equals(nodeObject.getId())) {
                outlineModel.getTreePathSupport().expandPath(treePath);
                List<Long> relativePath = expandedPath.subList(1, expandedPath.size());
                if (!relativePath.isEmpty()) {
                    relativeExpandedIds.add(relativePath);
                    log.trace(indent+"  relative expanded: "+relativePath);
                }
            }
        }
        
        if (!relativeExpandedIds.isEmpty()) {
            for(int i=0; i<outlineModel.getChildCount(nodeObject); i++) {
                TreePath childPath = treePath.pathByAddingChild(outlineModel.getChild(nodeObject, i));
                restoreExpansionState(childPath, relativeExpandedIds, indent+"----");
            }
        }
    }
}
