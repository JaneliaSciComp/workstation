package org.janelia.it.workstation.ab2.model;

/*

This class represents a neuron skeleton. The skeleton is a graph of hierachical nodes with parent-child relationships.

 */

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.janelia.geometry3d.Vector3;

public class AB2NeuronSkeleton {
    private ArrayDeque<Node> nodes=new ArrayDeque<>();

    public class Node {
        private double x;
        private double y;
        private double z;
        private ArrayDeque<Node> children=new ArrayDeque<>();
        private Node parent;

        public Node(double x, double y, double z, Node parent) {
            this.x=x;
            this.y=y;
            this.z=z;
            this.parent=parent;
        }

        public double x() { return x; }
        public double y() { return y; }
        public double z() { return z; }

        public Node getParent() { return parent; }

        public List<Node> getChildren() {
            List<Node> childList = new ArrayList<>();
            childList.addAll(children);
            return childList;
        }

    }

    public AB2NeuronSkeleton() {
    }

    public int getSize() { return nodes.size(); }

    public Node getRootNode() {
        if (nodes.size()==0) {
            return null;
        } else {
            return nodes.peekFirst();
        }
    }

    public Node addNode(double x, double y, double z, Node parent) throws Exception {
        if (parent==null && nodes.size()>0) {
            throw new Exception("Cannot have mulitple root nodes - parent must be non-null");
        } else if (parent==null) {
            Node rootNode=new Node(x,y,z,null);
            nodes.addFirst(rootNode);
            return rootNode;
        } else {
            if (!nodes.contains(parent)) {
                throw new Exception("Parent must already exist in skeleton");
            }
            Node newNode=new Node(x,y,z,parent);
            nodes.add(newNode);
            parent.children.add(newNode);
            return newNode;
        }
    }

    public Node updateNode(double x, double y, double z, Node node) {
        node.x=x;
        node.y=y;
        node.z=z;
        return node;
    }

    public void removeNode(Node node) {
        if (nodes.contains(node)) {
            for (Node child : node.children) {
                removeNode(child);
            }
        }
        Node parent=node.parent;
        if (parent!=null) {
            parent.children.remove(node);
        }
        nodes.remove(node);
    }

    public List<Node> getLeafNodes() {
        List<Node> leafNodes=new ArrayList<>();
        for (Node node : nodes) {
            if (node.getParent()!=null && node.children.size()==0) {
                leafNodes.add(node);
            }
        }
        return leafNodes;
    }

    public List<Vector3> getSkeletonPointSet() {
        List<Vector3> pointList=new ArrayList<>();
        getSkeletonPointInfo(getRootNode(), pointList);
        return pointList;
    }

    private void getSkeletonPointInfo(AB2NeuronSkeleton.Node node, List<Vector3> pointList) {
        if (node==null) return;
        pointList.add(new Vector3((float)node.x(), (float)node.y(), (float)node.z()));
        List<AB2NeuronSkeleton.Node> children=node.getChildren();
        if (children!=null && children.size()>0) {
            for (AB2NeuronSkeleton.Node child : children) {
                getSkeletonPointInfo(child, pointList);
            }
        }
    }

    public List<Vector3> getSkeletonLineSet() {
        List<Vector3> lineList=new ArrayList<>();
        getSkeletonLineInfo(getRootNode(), lineList);
        return lineList;
    }

    private void getSkeletonLineInfo(AB2NeuronSkeleton.Node node, List<Vector3> lineList) {
        if (node==null) return;
        List<AB2NeuronSkeleton.Node> children=node.getChildren();
        if (children!=null && children.size()>0) {
            for (AB2NeuronSkeleton.Node child : children) {
                lineList.add(new Vector3((float)node.x(), (float)node.y(), (float)node.z()));
                lineList.add(new Vector3((float)child.x(), (float)child.y(), (float)child.z()));
                getSkeletonLineInfo(child, lineList);
            }
        }
    }

}
