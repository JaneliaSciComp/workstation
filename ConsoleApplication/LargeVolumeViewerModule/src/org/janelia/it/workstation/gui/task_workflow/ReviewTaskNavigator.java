package org.janelia.it.workstation.gui.task_workflow;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.*;
import com.mxgraph.*;
import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger log = LoggerFactory.getLogger(ReviewTaskNavigator.class);
    mxGraph graph;
    mxGraphComponent graphComponent;
    Object parent;
    
    static final int HORIZ_OFFSET = 25;
    static final int VERT_OFFSET = 15;
    public enum CELL_STATUS {
        OPEN, UNDER_REVIEW, REVIEWED
    };
    
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
            traceNode (0, null, tree, 250,0);
        } finally {
            graph.getModel().endUpdate();
        }

        graphComponent = new mxGraphComponent(graph);
        graphComponent.addMouseWheelListener(this);
        graphComponent.setPanning(true);
        return graphComponent;
    }
    
    private void traceNode(int level, mxCell prevPoint, NeuronTree node, int currx, int curry) {
        // add current node
        mxCell nodePoint = (mxCell) graph.insertVertex(parent, null, "", currx, curry, 10,
                10, "orthogonal=true;shape=ellipse;perimeter=ellipsePerimeter;fillColor=red");
        node.setGUICell(nodePoint);
        if (prevPoint != null)
            graph.insertEdge(parent, null, "", prevPoint, nodePoint, "style=orthogonal;strokeWidth=3;endArrow=none;strokeColor=green");
        List<NeuronTree> childNodes = node.getChildren();
        if (node.isLeaf())
            return;


        // layout offsets based off children node's leaf width
        currx += (int)(-HORIZ_OFFSET * node.getWidth() * 0.5);
        curry += VERT_OFFSET;

        for (int i=0; i<childNodes.size(); i++) {
            NeuronTree childNode = childNodes.get(i);
            int centerOffset = (int)(HORIZ_OFFSET * childNode.getWidth() * .5);
            currx += centerOffset;
            traceNode (level+1,nodePoint, childNode, currx, curry);
            currx += centerOffset;

        }        
    }
    
    public void updateCellStatus (Object[] cells, CELL_STATUS status) {   
        graph.getModel().beginUpdate();
        try {
            switch (status) {
                case OPEN:
                    graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#ff0000", cells);
                    break;
                case UNDER_REVIEW:
                    graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#ffff00", cells);
                    break;
                case REVIEWED:                    
                    graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#ffffff", cells);
                    break;
            }
        } finally {
            graph.getModel().endUpdate();
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
