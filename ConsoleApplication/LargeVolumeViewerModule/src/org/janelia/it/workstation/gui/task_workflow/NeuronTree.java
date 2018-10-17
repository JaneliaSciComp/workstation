package org.janelia.it.workstation.gui.task_workflow;

import com.mxgraph.model.mxCell;
import java.util.ArrayList;
import java.util.List;
import org.janelia.it.jacs.shared.geom.Vec3;

/**
 *
 * @author schauderd
 */
public class NeuronTree implements PointDisplay {
    private NeuronTree parent;
    private Vec3 loc;
    private List<NeuronTree> children;
    private boolean visited;
    private mxCell cell;

    public NeuronTree(NeuronTree parentNode, Vec3 vertexLoc) {
        parent = parentNode;       
        loc = vertexLoc;
        children = new ArrayList<NeuronTree>();        
        visited = false;
    }
    
    public void addChild (NeuronTree tree) {
        children.add(tree);
    }
    
    public List<NeuronTree> getChildren() {
        return children;
    }
    
    public NeuronTree getParent() {
        return parent;
    }
    
    @Override
    public Vec3 getVertexLocation() {
        return loc;
    }
    
    public List<PointDisplay> generateRootToLeaf() {
        List<PointDisplay> rootToLeaf = new ArrayList<PointDisplay>();        
        NeuronTree currentNode = this;
        while (currentNode.getParent()!=null && !currentNode.getParent().getVisited()) {
             rootToLeaf.add(0, currentNode);
             currentNode = currentNode.getParent();
             currentNode.setVisited(true);
        }               
        return rootToLeaf;
    }
    
    public boolean isLeaf() {        
        if (children.size()==0)
            return true;
        return false;
    }
    
    public boolean getVisited () {
        return visited;
    }
    
    public void setVisited (boolean visited) {
        this.visited = visited;
    }
    
    public boolean isBranchingNode() {        
        if (children.size()>1)
            return true;
        return false;
    }

    public mxCell getGUICell() {
        return cell;
    }

    public void setGUICell(mxCell cell) {
        this.cell = cell;
    }
    
    @Override
    public void setReviewed(boolean review) {
        if (review) {
            cell.setStyle("fillColor=red");
        } else {
            cell.setStyle("fillColor=green");
        }
    }  
}