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

public class AB2SimulatedNeuronSkeletonGenerator {
    private Random random;

    private double branchProbability=0.05;
    private double initialBranchProbability=0.5;
    private double stepLength=0.003;
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
        while(skeleton.getSize()<nodeCount) {
            List<AB2NeuronSkeleton.Node> leafNodes=skeleton.getLeafNodes();
            int leafIndex=random.nextInt(leafNodes.size());
            AB2NeuronSkeleton.Node nodeToExtend=leafNodes.get(leafIndex);
            AB2NeuronSkeleton.Node parentNode=nodeToExtend.getParent();
            Vector3 childDirection=getVectorFromNodes(parentNode, nodeToExtend);
            // Because we don't need to change the length of the vector and just perturb its
            // position, we can use 2-parameter polar coordinates
            Double thetaDelta=random.nextDouble()*stepAngleLimitRadians - stepAngleLimitRadians/2.0;
            Double phiDelta=random.nextDouble()*stepAngleLimitRadians - stepAngleLimitRadians/2.0;
            Vector3 childDirectionSpherical=getSphericalVectorFromCartesianVector(childDirection);
            Vector3 modifiedChildDirectionSpherical=new Vector3(
                    (float)(childDirectionSpherical.getX()),
                    (float)(childDirectionSpherical.getY()+thetaDelta),
                    (float)(childDirectionSpherical.getZ()+phiDelta));
            Vector3 nextVector=getCartesianVectorFromSpherical(modifiedChildDirectionSpherical);
            double boundary=stepLength*5;
            if (nextVector.getX()<(boundingBox[0]+boundary) || nextVector.getX()>(boundingBox[3]-boundary)
                || nextVector.getY()<(boundingBox[1]+boundary) || nextVector.getY()>(boundingBox[4]-boundary)
                    || nextVector.getZ()<(boundingBox[2]+boundary) || nextVector.getZ()>(boundingBox[5]-boundary)) {
                continue; // out of bounds, discard
            }
            AB2NeuronSkeleton.Node newNode = skeleton.addNode(nodeToExtend.x()+nextVector.getX(),
                    nodeToExtend.y()+nextVector.getY(), nodeToExtend.z()+nextVector.getZ(), nodeToExtend);
            if (skeleton.getSize()<(nodeCount-1)) {
                if (random.nextDouble()<branchProbability) {
                    addBranchNodes(newNode, true);
                }
            }
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
        Vector3 rV=getScaledVectorFromNorms(rdX, rdY, rdZ);
        rV=rV.normalize();
        // Next, get the inverse vector
        Vector3 rVi=rV.negate();
        // Scale to proper increment
        rV=rV.multiplyScalar((float)stepLength);
        rVi=rVi.multiplyScalar((float)stepLength);
        // Add the two child nodes
        skeleton.addNode(parentNode.x()+rV.getX(), parentNode.y()+rV.getY(), parentNode.z()+rV.getZ(), parentNode);
        if (addSecondBranch) {
            skeleton.addNode(parentNode.x() + rVi.getX(), parentNode.y() + rVi.getY(), parentNode.z() + rVi.getZ(), parentNode);
        }
    }

    public Vector3 getScaledVectorFromNorms(double x, double y, double z) {
        double bbXstart=boundingBox[0];
        double bbXlength=boundingBox[3]-boundingBox[0];
        double bbYstart=boundingBox[1];
        double bbYlength=boundingBox[4]-boundingBox[1];
        double bbZstart=boundingBox[2];
        double bbZlength=boundingBox[5]-boundingBox[2];
        Vector3 scaledVector=new Vector3(
                (float)(x/bbXlength+bbXstart),
                (float)(y/bbYlength+bbYlength),
                (float)(z/bbZlength+bbZlength));
        return scaledVector;
    }


}
