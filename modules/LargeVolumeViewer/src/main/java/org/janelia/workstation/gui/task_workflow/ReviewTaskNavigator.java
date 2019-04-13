package org.janelia.workstation.gui.task_workflow;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.*;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.handler.mxPanningHandler;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxRectangle;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;

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
    List<Object> edges;
    List<Object> cells;
    
    static final int HORIZ_OFFSET = 25;
    static final int VERT_OFFSET = 15;
    public enum CELL_STATUS {
        OPEN, UNDER_REVIEW, REVIEWED
    };
    
    ReviewTaskNavigator() {
        
    }
    
    public void addCellListener (mxIEventListener listener) {
        graph.getSelectionModel().addListener(mxEvent.CHANGE, listener);
    }
    
    public JScrollPane createGraph(NeuronTree tree, int leaves, int width, int height) {
        edges = new ArrayList<Object>();
        cells = new ArrayList<Object>();
        graph = new mxGraph() { 
            @Override
            public boolean isCellEditable(Object cell){                
                return false; 
            } 
            
            @Override
            public boolean isCellSelectable(Object cell) {                 
                if (cells.contains(cell))
                    return true;
                else return false;
            } 
            
            @Override
            public boolean isCellMovable(Object cell){                
                return false; 
            }     
        };

        parent = graph.getDefaultParent();
        
        graph.getModel().beginUpdate();
        try {
            traceNode (0, null, tree, (int)(leaves*HORIZ_OFFSET/2),0);
            graph.orderCells(true, edges.toArray());
        } finally {
            graph.getModel().endUpdate();
        }
        
        mxRectangle bounds = graph.getGraphBounds();
        
        graphComponent = new mxGraphComponent(graph) {
            @Override 
            public boolean isPanningEvent(MouseEvent evt) {
                return true;
            }
            
            @Override 
            protected mxPanningHandler createPanningHandler() {
                return new mxPanningHandler(this) {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        if (!e.isConsumed() && start != null) {
                            int dx = e.getX() - start.x;
                            int dy = e.getY() - start.y;
                            
                            Rectangle r = graphComponent.getViewport().getViewRect();
                            
                            int right = r.x + ((dx > 0) ? 0 : r.width) - dx;
                            int bottom = r.y + ((dy > 0) ? 0 : r.height) - dy;
                            
                            graphComponent.getGraphControl().scrollRectToVisible(
                                    new Rectangle(right, bottom, 0, 0));

                            e.consume();
                        }
                    }
                };
            }
        };
        graphComponent.addMouseWheelListener(this);
        double largestDim = bounds.getHeight();
        if (largestDim>bounds.getWidth()) {
            graphComponent.zoom(height/bounds.getHeight());
        } else {
            graphComponent.zoom(width/bounds.getWidth());
        }
        graphComponent.scrollCellToVisible(tree.getGUICell());
        graphComponent.scrollToCenter(true);
        graphComponent.setPanning(true);
        graphComponent.setAutoExtend(true);
        graphComponent.refresh();

        return graphComponent;        
    }
    
    private void traceNode(int level, mxCell prevPoint, NeuronTree node, int currx, int curry) {
        // add current node
        mxCell nodePoint = (mxCell) graph.insertVertex(parent, null, "", currx, curry, 10,
                10, "orthogonal=true;shape=ellipse;perimeter=ellipsePerimeter;fillColor=red");
        cells.add(nodePoint);
        node.setGUICell(nodePoint);
        if (prevPoint != null) {
            Object edge = graph.insertEdge(parent, null, "", prevPoint, nodePoint, "style=orthogonal;strokeWidth=3;endArrow=none;strokeColor=green");
            edges.add(edge);
        }
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
                    graph.setCellStyles(mxConstants.STYLE_SHAPE, "ellipse", cells);                    
                    break;
                case UNDER_REVIEW:
                    graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#ffff00", cells);   
                    break;
                case REVIEWED:                    
                    graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#ffffff", cells);
                    graph.setCellStyles(mxConstants.STYLE_SHAPE, "rectangle", cells);
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
