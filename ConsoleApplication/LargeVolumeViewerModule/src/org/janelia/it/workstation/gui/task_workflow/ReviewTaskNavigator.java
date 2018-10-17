package org.janelia.it.workstation.gui.task_workflow;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.*;
import com.mxgraph.*;
import com.mxgraph.model.mxCell;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.List;
import java.util.stream.IntStream;
import javax.swing.JScrollPane;
/**
 *
 * @author schauderd
 */


public class ReviewTaskNavigator implements MouseWheelListener {
    mxGraph graph;
    mxGraphComponent graphComponent;
    Object parent;
    
    static final int HORIZ_OFFSET = 25;
    static final int VERT_OFFSET = 15;
    
    ReviewTaskNavigator() {
        
    }
    
    public JScrollPane createGraph(NeuronTree tree, int width) {
        graph = new mxGraph() { 
            @Override
            public boolean isCellEditable(Object cell){ 
                if (cell instanceof mxCell) { 
                    mxCell c = (mxCell)cell; 
                    if (c.isEdge()) { 
                        return false; 
                    } else  
                        return true;                     
                } 
                return false; 
            } 
            
            @Override
            public boolean isCellSelectable(Object cell){ 
                if (cell instanceof mxCell) { 
                    mxCell c = (mxCell)cell;                    
                    if (c.isEdge()) { 
                        return false; 
                    } else  
                        return true;                     
                } 
                return false; 
            } 
        };
        graph.orderCells(false);
        parent = graph.getDefaultParent();

        graph.getModel().beginUpdate();
        try {
            traceNode (null, tree, (int)Math.floor(width/2),0);
        } finally {
            graph.getModel().endUpdate();
        }

        graphComponent = new mxGraphComponent(graph);
        graphComponent.addMouseWheelListener(this);
        graphComponent.setPanning(true);
        return graphComponent;
    }
    
    private int calcLeaves (NeuronTree node) {
        int leaves = 0;
        if (node.isLeaf()) { 
            return 1;            
        }
       
        for (int i=0; i<node.getChildren().size(); i++) {
            NeuronTree childNode = node.getChildren().get(i);
            leaves += calcLeaves (childNode);
        }
        return leaves;
    }
    
    private void traceNode(mxCell prevPoint, NeuronTree node, int currx, int curry) {
        // add current node
        mxCell nodePoint = (mxCell)graph.insertVertex(parent, null, "", currx, curry, 10,
                            10, "orthogonal=true;shape=ellipse;perimeter=ellipsePerimeter;fillColor=red");
        if (prevPoint!=null)
            graph.insertEdge(parent, null, "", prevPoint, nodePoint,"style=orthogonal;strokeWidth=3;endArrow=none;strokeColor=green");              
        List<NeuronTree> childNodes = node.getChildren();
        if (node.isLeaf()) 
            return;        
        // slightly wacky, we need to find the total different branches to calculate the offset properly
        int leaves = calcLeaves(node);        
        currx += (int)(-HORIZ_OFFSET * (leaves - 1) * 0.5);
        curry += VERT_OFFSET;
        for (int i=0; i<childNodes.size(); i++) {
            NeuronTree childNode = childNodes.get(i);            
            currx += (int)(HORIZ_OFFSET * calcLeaves(childNode) * 0.5);
            traceNode (nodePoint, childNode, currx, curry);
        }        
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getWheelRotation() < 0){
            graphComponent.zoomIn();
        } else {
            graphComponent.zoomOut();
        }
    }
}
