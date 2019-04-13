package org.janelia.workstation.gui.task_workflow;

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
    private Long annotationId;
    private List<NeuronTree> children;
    private int width; // number of leaves underneath this node
    private boolean visited;
    private mxCell cell;
    private boolean reviewed;

    public NeuronTree(NeuronTree parentNode, Vec3 vertexLoc, Long annotation) {
        parent = parentNode;       
        loc = vertexLoc;
        annotationId = annotation;
        children = new ArrayList<NeuronTree>();        
        visited = false;
        width = 0;
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
        rootToLeaf.add(0, currentNode);
        currentNode.setVisited(true);
        while (currentNode.getParent()!=null) {
             if (!currentNode.getVisited()) {
                 rootToLeaf.add(0, currentNode);
                 currentNode.setVisited(true);
             }
             currentNode.setWidth(currentNode.getWidth()+1);
             currentNode = currentNode.getParent();
        }
        if (currentNode.getParent()==null && !currentNode.getVisited()) {
            rootToLeaf.add(0, currentNode);
            currentNode.setVisited(true);
        }
        currentNode.setWidth(currentNode.getWidth()+1);

        return rootToLeaf;
    }
    
    public boolean isLeaf() {        
        if (children.size()==0)
            return true;
        return false;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
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

    public Long getAnnotationId() {
        return annotationId;
    }

    public void setAnnotationId(Long annotationId) {
        this.annotationId = annotationId;
    }    

    public mxCell getGUICell() {
        return cell;
    }

    public void setGUICell(mxCell cell) {
        this.cell = cell;
    }
    
    @Override
    public boolean isReviewed() {
        return reviewed;
    }
    
    @Override
    public void setReviewed(boolean review) {
        reviewed = review;
        if (review) {
            cell.setStyle("fillColor=red");
        } else {
            cell.setStyle("fillColor=green");
        }
    }  
}