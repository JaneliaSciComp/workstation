package org.janelia.it.workstation.ab2.compute;


/*

This class walks a skeleton at a specified step resolution. It traverses all vertices and edges, and
knows which nodes it is between, and what fraction of the distance along each edge. It is given a
callback that it calls at each point to do work.

*/


import java.util.List;

import org.janelia.geometry3d.Vector3;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.model.AB2NeuronSkeleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2SkeletonWalker {

    Logger logger= LoggerFactory.getLogger(AB2SkeletonWalker.class);

    AB2NeuronSkeleton skeleton;
    double normalizedStepSize;

    public AB2SkeletonWalker(AB2NeuronSkeleton skeleton, double normalizedStepSize) {
        this.skeleton=skeleton;
        this.normalizedStepSize=normalizedStepSize;
    }

    public void walkSkeleton(AB2SkeletonWalkerCallback walkerCallback) {
        if (skeleton==null) {
            logger.error("skeleton is null");
        }
        if (walkerCallback==null) {
            logger.error("walkerCallback is null");
        }
        processNode(skeleton.getRootNode(), walkerCallback);
    }

    /*

    This recursive method processes all edges beginning with the given node, and extending to each of its
    child nodes, up until the child node itself, which instead of being processed as a position, is
    handled by recursively calling that node with this method.

     */
    protected void processNode(AB2NeuronSkeleton.Node node, AB2SkeletonWalkerCallback walkerCallback) {
        // First, process the node at this position. If this is a leaf node, we are done.
        walkerCallback.processPosition(node, null, 0.0);
        List<AB2NeuronSkeleton.Node> children=node.getChildren();
        if (children!=null && children.size()>0) {
            Vector3 parentPosition=new Vector3((float)node.x(), (float)node.y(), (float)node.z());
            for (AB2NeuronSkeleton.Node child : children) {
                Vector3 childPosition=new Vector3((float)child.x(), (float)child.y(), (float)child.z());
                Vector3 edge=childPosition.minus(parentPosition);
                float edgeLength=edge.length();
                int steps=(int)(edgeLength/(float)normalizedStepSize);
                if (steps<1) steps=1;// minimum
                float segmentLength=edgeLength/((steps+1)*1.0f);
                float segmentPerc=segmentLength/edgeLength;
                for (int i=0;i<steps;i++) {
                    walkerCallback.processPosition(node, child, (segmentPerc*((i+1)*1f)));
                }
                processNode(child, walkerCallback);
            }
        }
    }

}
