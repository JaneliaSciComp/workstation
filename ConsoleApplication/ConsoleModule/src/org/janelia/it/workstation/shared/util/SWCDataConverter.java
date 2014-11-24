package org.janelia.it.workstation.shared.util;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.workstation.geom.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmAnchoredPath;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmAnchoredPathEndpoints;

/**
 * this class handles the details of translating TmNeurons into SWCData
 */
public class SWCDataConverter {

    public static SWCData fromTmNeuron(TmNeuron neuron) {
        return fromTmNeuron(neuron, neuronCenterOfMass(neuron), 1);
    }

    public static SWCData fromTmNeuron(TmNeuron neuron, int downsampleModulo) {
        return fromTmNeuron(neuron, neuronCenterOfMass(neuron), downsampleModulo);
    }

    public static SWCData fromTmNeuron(TmNeuron neuron, Vec3 center, int downsampleModulo) {

        List<String> headerList = new ArrayList<>();

        double xcenter = center.getX();
        double ycenter = center.getY();
        double zcenter = center.getZ();
        List<SWCNode> nodeList = null;
        if (downsampleModulo == 0) {
            nodeList = nodesFromSubtrees(neuron, xcenter, ycenter, zcenter);
        }        
        else {
            nodeList = nodesFromCombinedPath(neuron, xcenter, ycenter, zcenter, downsampleModulo);
        }
        
        // headers: I'm not going to put in all the fields I
        //  saw in the "specification" unless I have to; we only
        //  use the OFFSET field
        headerList.add("# ORIGINAL_SOURCE Janelia Workstation Large Volume Viewer");
        headerList.add(String.format("# OFFSET %f %f %f", xcenter, ycenter, zcenter));

        return new SWCData(nodeList, headerList);
    }

    public static SWCData fromTmNeuron(List<TmNeuron> neuronList, int downsampleModulo) {
        Vec3 com = neuronCenterOfMass(neuronList);

        List<SWCData> dataList = new ArrayList<>();
        for (TmNeuron neuron: neuronList) {
            if (neuron != null && neuron.getGeoAnnotationMap().size() > 0) {
                dataList.add(fromTmNeuron(neuron, com, downsampleModulo));
            }
        }

        // if more than one in list, merge everything into first one
        if (dataList.size() == 0) {
            return null;
        } else if (dataList.size() == 1) {
            return dataList.get(0);
        } else {
            // merge all to first
            for (int i=1; i < dataList.size(); i++) {
                dataList.get(0).addDataFrom(dataList.get(i));
            }
            return dataList.get(0);
        }
    }

    public static Vec3 neuronCenterOfMass(TmNeuron neuron) {
        // it is probably not strictly correct, but I'm going
        //  to return the origin for center if the neuron
        //  is null or has no points

        double sumx = 0.0;
        double sumy = 0.0;
        double sumz = 0.0;

        if (neuron == null) {
            return new Vec3(sumx, sumy, sumz);
        }

        double length = neuron.getGeoAnnotationMap().size();
        if (length == 0) {
            return new Vec3(sumx, sumy, sumz);
        }

        for (TmGeoAnnotation ann: neuron.getGeoAnnotationMap().values()) {
            sumx += ann.getX();
            sumy += ann.getY();
            sumz += ann.getZ();
        }
        return new Vec3(sumx / length, sumy / length, sumz / length);
    }

    public static Vec3 neuronCenterOfMass(List<TmNeuron> neuronList) {

        Vec3 com;
        Vec3 sum = new Vec3(0.0, 0.0, 0.0);

        int nNodes;
        int totalNodes = 0;

        for (TmNeuron neuron: neuronList) {
            if (neuron == null) {
                continue;
            }
            nNodes = neuron.getGeoAnnotationMap().size();
            if (nNodes == 0) {
                continue;
            }
            com = neuronCenterOfMass(neuron);
            com.multEquals(nNodes);
            sum.plusEquals(com);
            totalNodes += nNodes;
        }

        if (totalNodes > 0) {
            sum.multEquals(1.0 / totalNodes);
        }
        // note that if totalNodes is zero, sum is also all zeros
        return sum;
    }

    private static List<SWCNode> nodesFromCombinedPath(TmNeuron neuron, double xcenter, double ycenter, double zcenter, int downsampleModulo) {
        List<SWCNode> nodeList = new ArrayList<>();
        
        // Find the links back to the auto-generated points lists, from the
        // manual-added points.
        Map<TmAnchoredPathEndpoints, TmAnchoredPath> map = neuron.getAnchoredPathMap();
        Map<Long,TmAnchoredPathEndpoints> startToEndPoints = new HashMap<>();
        Map<Long,Integer> subAnnIdToIndex = new HashMap<>();
        for (TmAnchoredPathEndpoints endPoints : map.keySet()) {
            startToEndPoints.put(endPoints.getAnnotationID2(), endPoints);
        }

        int currentIndex = 1;
        int parentIndex = -1;
        
        for (TmGeoAnnotation annotation : neuron.getRootAnnotations()) {
            for (TmGeoAnnotation subAnn : neuron.getSubTreeList(annotation)) {
                // Traverse any "parent points" to this end-point.
                if ( startToEndPoints.get( subAnn.getId() ) != null ) {
                    final TmAnchoredPathEndpoints endpoints = startToEndPoints.get( subAnn.getId() );
                    // Make a node for each path member.
                    TmAnchoredPath anchoredPath = map.get( endpoints );                    
                    if ( subAnnIdToIndex.get( endpoints.getAnnotationID1() ) != null ) {
                        parentIndex = subAnnIdToIndex.get( endpoints.getAnnotationID1() );
                    }

                    for (int inListNodeNum = anchoredPath.getPointList().size() - 2; inListNodeNum > 0 ; inListNodeNum--) {
                        if (inListNodeNum % downsampleModulo != 0) {
                            continue;
                        }
                        List<Integer> point = anchoredPath.getPointList().get(inListNodeNum);
                        SWCNode autoNode = createSWCNode(
                                currentIndex,
                                SWCNode.SegmentType.undefined,
                                point.get(0),
                                point.get(1),
                                point.get(2),
                                xcenter,
                                ycenter,
                                zcenter,
                                parentIndex
                        );
System.out.println("Just constructed auto-node with parent of " + autoNode.getParentIndex() + " and actual index of " + autoNode.getIndex()); 

                        nodeList.add(autoNode);
                        parentIndex = currentIndex;
                        currentIndex++;
                    }

                }
                
//                if (subAnnIdToIndex.get(subAnn.getParentId()) != null) {
//                    parentIndex = subAnnIdToIndex.get(subAnn.getParentId());
//System.out.println("Fetched parent index of " + parentIndex + " for sub-annotation " + subAnn.getId() + " with parent id of " + subAnn.getParentId());
//                }

                // Make the node for manual reference now.                
                SWCNode manualNode = createSWCNode(
                        currentIndex++,
                        getSegmentType(subAnn),
                        subAnn.getX(),
                        subAnn.getY(),
                        subAnn.getZ(),
                        xcenter,
                        ycenter,
                        zcenter,
                        parentIndex
                );
                nodeList.add(manualNode);                
                subAnnIdToIndex.put( subAnn.getId(), manualNode.getIndex() );
                parentIndex = manualNode.getIndex();

System.out.println("Just constructed manual-node with parent of " + manualNode.getParentIndex() + " and actual index of " + manualNode.getIndex()); 
            }
        }

        Comparator<SWCNode> indexComparator = new Comparator<SWCNode>() {
            @Override
            public int compare(SWCNode o1, SWCNode o2) {
                return o1.getIndex() - o2.getIndex();
            }            
        };        
        Collections.sort(nodeList, indexComparator);
        return nodeList;
    }
    

    
    private static List<SWCNode> nodesFromAnchoredPath(TmNeuron neuron, double xcenter, double ycenter, double zcenter, int downsampleModulo) {
        List<SWCNode> nodeList = new ArrayList<>();
        // map the annotation IDs to the indices to be used in the swc file
        Map<Long, Integer> annoToIndex = new HashMap<>();

        Map<TmAnchoredPathEndpoints, TmAnchoredPath> map = neuron.getAnchoredPathMap();
        int currentIndex = 1;
        int parentIndex = -1;
        Map<Long,Long> annoToParent = new HashMap<>();
        Map<Long, SWCNode> manualIdToNode = new HashMap<>();
        
        // Go through annotations to find linkage relationships.
        for (TmGeoAnnotation annotation : neuron.getRootAnnotations()) {
            for (TmGeoAnnotation subAnn : neuron.getSubTreeList(annotation)) {
                annoToParent.put(subAnn.getId(), subAnn.getParentId());
                annoToIndex.put(subAnn.getId(), currentIndex);
                currentIndex++;
            }
        }
        
        for (TmGeoAnnotation annotation : neuron.getRootAnnotations()) {
            for (TmGeoAnnotation subAnn : neuron.getSubTreeList(annotation)) {
                Integer parentIdInteger = annoToIndex.get(subAnn.getParentId());
                if (parentIdInteger != null) {
                    parentIndex = parentIdInteger;
                }

                if (subAnn.isRoot()) {
                    parentIndex = -1;
                }

                final Integer index = annoToIndex.get(subAnn.getId());
                SWCNode swcNode = createSWCNode(
                        index,
                        getSegmentType(subAnn),
                        subAnn.getX(),
                        subAnn.getY(),
                        subAnn.getZ(),
                        xcenter,
                        ycenter,
                        zcenter,
                        parentIndex
                );                
                nodeList.add(swcNode);
                manualIdToNode.put(subAnn.getId(), swcNode);
                parentIndex = index;
            }
        }

        for (TmAnchoredPathEndpoints endPoints : map.keySet()) {
            // Make a node for each path member.
            TmAnchoredPath anchoredPath = map.get(endPoints);
            parentIndex = currentIndex - 1;

            int firstIndex = -1;
            SWCNode lastNode;
            SWCNode swcNode = null;

            for (int inListNodeNum = 1; inListNodeNum < anchoredPath.getPointList().size() - 1; inListNodeNum++) {
                if (inListNodeNum % downsampleModulo != 0) {
                    continue;
                }
                List<Integer> point = anchoredPath.getPointList().get(inListNodeNum);
                swcNode = createSWCNode(
                        currentIndex,
                        SWCNode.SegmentType.undefined,
                        point.get(0),
                        point.get(1),
                        point.get(2),
                        xcenter,
                        ycenter,
                        zcenter,
                        parentIndex
                );

                nodeList.add(swcNode);
                if (firstIndex == -1) {
                    firstIndex = currentIndex;
                }
                parentIndex = currentIndex;
                currentIndex++;
            }
            lastNode = swcNode;

            // Establish link-arounds.
            SWCNode manualNode = manualIdToNode.get(endPoints.getAnnotationID2());
            manualNode.setParentIndex(firstIndex);

            if (lastNode != null) {
                manualNode = manualIdToNode.get(endPoints.getAnnotationID1());
                lastNode.setParentIndex(manualNode.getIndex());
            }
        }

        Comparator<SWCNode> indexComparator = new Comparator<SWCNode>() {
            @Override
            public int compare(SWCNode o1, SWCNode o2) {
                return o1.getIndex() - o2.getIndex();
            }            
        };        
        Collections.sort(nodeList, indexComparator);
        return nodeList;
    }
    
    private static List<SWCNode> nodesFromSubtrees(TmNeuron neuron, double xcenter, double ycenter, double zcenter) {
        List<SWCNode> nodeList = new ArrayList<>();
        // map the annotation IDs to the indices to be used in the swc file
        Map<Long, Integer> annMap = new HashMap<>();

        int currentIndex = 1;
        int parentIndex;
        for (TmGeoAnnotation root: neuron.getRootAnnotations()) {
            for (TmGeoAnnotation ann: neuron.getSubTreeList(root)) {
                if (ann.isRoot()) {
                    parentIndex = -1;
                } else {
                    parentIndex = annMap.get(ann.getParentId());
                }
                SWCNode.SegmentType segmentType = getSegmentType(ann);

                nodeList.add(
                        createSWCNode(
                                currentIndex,
                                segmentType,
                                ann.getX(),
                                ann.getY(),
                                ann.getZ(),
                                xcenter,
                                ycenter, 
                                zcenter,
                                parentIndex
                        )
                );
                annMap.put(ann.getId(), currentIndex );
                currentIndex++;
            }
        }
        return nodeList;
    }

    private static SWCNode.SegmentType getSegmentType(TmGeoAnnotation ann) {
        SWCNode.SegmentType segmentType;
        // only marking "fork" and "end, as that's
        //  all we can surmise from geometry
        if (ann.getChildIds().size() == 0) {
            segmentType = SWCNode.SegmentType.end_point;
        } else if (ann.getChildIds().size() > 1) {
            segmentType = SWCNode.SegmentType.fork_point;
        } else {
            segmentType = SWCNode.SegmentType.undefined;
        }
        return segmentType;
    }

    /**
     * Factory method to encapsulate how to make a node, including the details
     * of relative position.
     * @return properly spec'd node.
     */
    private static SWCNode createSWCNode(
            int currentIndex,
            SWCNode.SegmentType segmentType,
            double xAnno,
            double yAnno,
            double zAnno,
            double xcenter,
            double ycenter,
            double zcenter,
            int parentIndex) {
        return new SWCNode(
                currentIndex,
                segmentType,
                xAnno - xcenter,
                yAnno - ycenter,
                zAnno - zcenter,
                1.0,    // radius, which we don't have right now
                parentIndex
        );
    }

}
