package org.janelia.it.workstation.ab2.test;

/*

This class generates a set of acyclic graphs that simulate the branching structure of neurons. It is intended
as a tool for developing visualization code.

 */


import java.util.Date;
import java.util.List;
import java.util.Random;

import org.janelia.geometry3d.Vector3;
import org.janelia.it.workstation.ab2.model.AB2NeuronSkeleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AB2SimulatedNeuronSkeletonGenerator {

    Logger logger= LoggerFactory.getLogger(AB2SimulatedNeuronSkeletonGenerator.class);

    private Random random;

    private double branchProbability=0.05;
    private double initialBranchProbability=0.5;
    private double stepLength=0.01;
    private double[] boundingBox={ 0.0, 0.0, 0.0, 1.0, 1.0, 1.0 };
    private double stepAngleLimitRadians=Math.PI/8.0;
    private int nodeCount=1000;
    private AB2NeuronSkeleton skeleton;

    public AB2SimulatedNeuronSkeletonGenerator() {
        random=new Random(new Date().getTime());
    }

    public AB2SimulatedNeuronSkeletonGenerator(long randomSeed) {
        random=new Random(randomSeed);
    }

    public AB2NeuronSkeleton generateSkeleton() throws Exception {
        skeleton=new AB2NeuronSkeleton();
        double rootX=random.nextDouble()*0.8+0.1; // so we aren't starting at the edge
        double rootY=random.nextDouble()*0.8+0.1;
        double rootZ=random.nextDouble()*0.8+0.1;
        skeleton.addNode(rootX, rootY, rootZ, null);
        AB2NeuronSkeleton.Node rootNode=skeleton.getRootNode();
        if (random.nextDouble()<initialBranchProbability) {
            addBranchNodes(rootNode, true);
        } else {
            addBranchNodes(rootNode, false);
        }
        // Enter main loop - obviously this is not performance optimized

        int DEBUG_COUNT=0;

        while(skeleton.getSize()<nodeCount) {
            List<AB2NeuronSkeleton.Node> leafNodes=skeleton.getLeafNodes();
            int leafIndex=random.nextInt(leafNodes.size());
            AB2NeuronSkeleton.Node nodeToExtend=leafNodes.get(leafIndex);
            AB2NeuronSkeleton.Node parentNode=nodeToExtend.getParent();
            //logger.info("parent x="+parentNode.x()+" y="+parentNode.y()+" z="+parentNode.z());
            //logger.info("exNode x="+nodeToExtend.x()+" y="+nodeToExtend.y()+" z="+nodeToExtend.z());
            //logger.info("Check8");
            Vector3 childDirection=getVectorFromNodes(parentNode, nodeToExtend);
            //logger.info("Check9");
            //logger.info("child x="+childDirection.getX()+" y="+childDirection.getY()+" z="+childDirection.getZ());
            // Because we don't need to change the length of the vector and just perturb its
            // position, we can use 2-parameter polar coordinates
            Double thetaDelta=random.nextDouble()*stepAngleLimitRadians - stepAngleLimitRadians/2.0;
            Double phiDelta=random.nextDouble()*stepAngleLimitRadians - stepAngleLimitRadians/2.0;
            //logger.info("thetaDelta="+thetaDelta+" phiDelta="+phiDelta);
            Vector3 childDirectionSpherical=getSphericalVectorFromCartesianVector(childDirection);
            //logger.info("child spherical x="+childDirectionSpherical.getX()+" y="+childDirectionSpherical.getY()+" z="+
            //                    childDirectionSpherical.getZ());
            //logger.info("Check10");
            Vector3 modifiedChildDirectionSpherical=new Vector3(
                    (float)(childDirectionSpherical.getX()),
                    (float)(childDirectionSpherical.getY()+thetaDelta),
                    (float)(childDirectionSpherical.getZ()+phiDelta));
            Vector3 nextVector=getCartesianVectorFromSpherical(modifiedChildDirectionSpherical);
            //logger.info("modifiedChildDirectionSpherical x="+modifiedChildDirectionSpherical.getX()+" y="+
            //        modifiedChildDirectionSpherical.getY()+" z="+modifiedChildDirectionSpherical.getZ());
            //logger.info("nextVector x="+nextVector.getX()+" y="+
            //        nextVector.getY()+" z="+nextVector.getZ());
            //logger.info("Check11");
            //DEBUG_COUNT++;
            //if (DEBUG_COUNT==5) {
            //    return null;
            //}
            double boundary=stepLength*5;
            Vector3 nodeToExtendPosition=new Vector3((float)nodeToExtend.x(), (float)nodeToExtend.y(), (float)nodeToExtend.z());
            nodeToExtendPosition.add(nextVector);
            if (nodeToExtendPosition.getX()<(boundingBox[0]+boundary) || nodeToExtendPosition.getX()>(boundingBox[3]-boundary)
                || nodeToExtendPosition.getY()<(boundingBox[1]+boundary) || nodeToExtendPosition.getY()>(boundingBox[4]-boundary)
                    || nodeToExtendPosition.getZ()<(boundingBox[2]+boundary) || nodeToExtendPosition.getZ()>(boundingBox[5]-boundary)) {
                continue; // out of bounds, discard
            }
            //logger.info("Check12");
            AB2NeuronSkeleton.Node newNode = skeleton.addNode(nodeToExtendPosition.getX(),
                    nodeToExtendPosition.getY(), nodeToExtendPosition.getZ(), nodeToExtend);
            //logger.info("Check13");
            if (skeleton.getSize()<(nodeCount-1)) {
                if (random.nextDouble()<branchProbability) {
                    //logger.info("Check14");
                    addBranchNodes(newNode, true);
                }
            }
            DEBUG_COUNT++;
            if (DEBUG_COUNT>nodeCount*2) {
                logger.error("Failed DEBUG_COUNT max of "+nodeCount*2);
                return null;
            }
            //logger.info("Check15 - skeleton.getSize()="+skeleton.getSize());
        }
        return skeleton;
    }

    private Vector3 getVectorFromNodes(AB2NeuronSkeleton.Node fromNode, AB2NeuronSkeleton.Node toNode) {
        Vector3 dV=new Vector3(
                (float)(toNode.x()-fromNode.x()),
                (float)(toNode.y()-fromNode.y()),
                (float)(toNode.z()-fromNode.z()));
        return dV;
    }

    private Vector3 getSphericalVectorFromCartesianVector(Vector3 c) {
        float r = c.length();
        float theta=(float)(Math.acos(c.getZ()/r));
        float phi=(float)(Math.atan2(c.getY(), c.getX()));
        return new Vector3(r, theta, phi);
    }

    private Vector3 getCartesianVectorFromSpherical(Vector3 s) {
        float x=(float)(s.getX()*Math.sin(s.getY())*Math.cos(s.getZ()));
        float y=(float)(s.getX()*Math.sin(s.getY())*Math.sin(s.getZ()));
        float z=(float)(s.getX()*Math.cos(s.getY()));
        return new Vector3(x, y, z);
    }

    private void addBranchNodes(AB2NeuronSkeleton.Node parentNode, boolean addSecondBranch) throws Exception {
        // We need two initial children
        // First, generate a random unit vector to pick a direction
        double rdX=random.nextDouble();
        double rdY=random.nextDouble();
        double rdZ=random.nextDouble();
        Vector3 rV=new Vector3((float)rdX, (float)rdY, (float)rdZ);
        rV.normalize();
        // Next, get the inverse vector
        Vector3 rVi=new Vector3(rV);
        rVi.negate();
        // Scale to proper increment
        rV=rV.multiplyScalar((float)stepLength);
        rVi=rVi.multiplyScalar((float)stepLength);
        // Add the two child nodes
        skeleton.addNode(parentNode.x()+rV.getX(), parentNode.y()+rV.getY(), parentNode.z()+rV.getZ(), parentNode);
        if (addSecondBranch) {
            skeleton.addNode(parentNode.x() + rVi.getX(), parentNode.y() + rVi.getY(), parentNode.z() + rVi.getZ(), parentNode);
        }
    }

}
