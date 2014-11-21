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
        return fromTmNeuron(neuron, neuronCenterOfMass(neuron));
    }

    public static SWCData fromTmNeuron(TmNeuron neuron, Vec3 center) {

        List<String> headerList = new ArrayList<>();

        double xcenter = center.getX();
        double ycenter = center.getY();
        double zcenter = center.getZ();
        //List<SWCNode> nodeList = nodesFromSubtrees(neuron, xcenter, ycenter, zcenter);

        List<SWCNode> altNodeList = nodesFromAnchoredPath(neuron, xcenter, ycenter, zcenter);
        
        // headers: I'm not going to put in all the fields I
        //  saw in the "specification" unless I have to; we only
        //  use the OFFSET field
        headerList.add("# ORIGINAL_SOURCE Janelia Workstation Large Volume Viewer");
        headerList.add(String.format("# OFFSET %f %f %f", xcenter, ycenter, zcenter));

//        return new SWCData(nodeList, headerList);
        return new SWCData(altNodeList, headerList);
    }

    public static SWCData fromTmNeuron(List<TmNeuron> neuronList) {
        Vec3 com = neuronCenterOfMass(neuronList);

        List<SWCData> dataList = new ArrayList<>();
        for (TmNeuron neuron: neuronList) {
            if (neuron != null && neuron.getGeoAnnotationMap().size() > 0) {
                dataList.add(fromTmNeuron(neuron, com));
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

    private static List<SWCNode> nodesFromAnchoredPath(TmNeuron neuron, double xcenter, double ycenter, double zcenter) {
        List<SWCNode> nodeList = new ArrayList<>();
        // map the annotation IDs to the indices to be used in the swc file
        Map<Long, Integer> annoToIndex = new HashMap<>();

        Map<TmAnchoredPathEndpoints, TmAnchoredPath> map = neuron.getAnchoredPathMap();
        int currentIndex = 1;
        int parentIndex = -1;
        List<Long> branchIds = new ArrayList<>();
        List<Long> rootIds = new ArrayList<>();
        Map<Long,Long> annoToParent = new HashMap<>();
        // Go through annotations to find linkage relationships.
        for (TmGeoAnnotation annotation : neuron.getRootAnnotations()) {
            for (TmGeoAnnotation subAnn : neuron.getSubTreeList(annotation)) {
                annoToParent.put(subAnn.getId(), subAnn.getParentId());
                annoToIndex.put(subAnn.getId(), currentIndex);
                currentIndex++;
                if (subAnn.isBranch()) {
                    branchIds.add(subAnn.getId());
                }
                if (subAnn.isRoot()) {
                    rootIds.add(subAnn.getId());
                }
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
                nodeList.add( swcNode );
                parentIndex = index;
            }
        }
        
        for ( TmAnchoredPathEndpoints endPoints: map.keySet() ) {
            // Make a node for each path member.
            TmAnchoredPath anchoredPath = map.get( endPoints );
            int inListNodeNum = 0;
            parentIndex = -1;
            for (List<Integer> point: anchoredPath.getPointList()) {
                SWCNode.SegmentType segmentType = null;
                segmentType = SWCNode.SegmentType.undefined;
                // Try both ends of the end-points, to find a parent.
                if (parentIndex == -1) {
                    Integer parentIndexInteger = annoToIndex.get(endPoints.getAnnotationID1());
                    if (parentIndexInteger == null) {
                        parentIndexInteger = annoToIndex.get(endPoints.getAnnotationID2());
                    }
                    if (parentIndexInteger != null) {
                        parentIndex = parentIndexInteger;
                    }
                }
                
                // Do not re-create any root or branch nodes.
                if (inListNodeNum == 0) {
                    final Long annotationId = endPoints.getAnnotationID1();
                    // Starting the list is the Anno-1 ID.
                    if ( branchIds.contains( endPoints.getAnnotationID1() ) ) {
                        continue;
                    }
                    else if ( rootIds.contains( annotationId) ) {
                        continue;
                    }
                }
                else if (inListNodeNum == anchoredPath.getPointList().size() - 1) {
                    final Long annotationId = endPoints.getAnnotationID2();
                    // And ending the list is the Anno-2 ID.
                    if ( rootIds.contains( annotationId) ) {
                        continue;
                    }
                    else if ( branchIds.contains( endPoints.getAnnotationID2() ) ) {
                        continue;
                    }
                }

                nodeList.add(
                        createSWCNode(
                                currentIndex,
                                segmentType,
                                point.get(0),
                                point.get(1),
                                point.get(2),
                                xcenter,
                                ycenter, 
                                zcenter,
                                parentIndex
                        )
                );
                parentIndex = currentIndex;
                
                currentIndex ++;

                inListNodeNum ++;
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
                annMap.put(ann.getId(), currentIndex);
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
