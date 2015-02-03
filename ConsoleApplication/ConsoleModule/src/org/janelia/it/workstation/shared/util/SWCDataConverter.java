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

    private ImportExportSWCExchanger exchanger;
    
    public void setSWCExchanger( ImportExportSWCExchanger exchanger ) {
        this.exchanger = exchanger;
    }
    
    public double[] internalFromExternal(double[] externalValue) {
        double[] rtnVal = exchanger.getInternal(externalValue);
        for ( int i = 0; i < rtnVal.length; i++ ) {
            rtnVal[ i ] = Math.round( rtnVal[ i ] );
        }
        return rtnVal;
    }
    
    public SWCData fromTmNeuron(TmNeuron neuron) {
        return fromTmNeuron(neuron, neuronCenterOfMass(neuron), 1);
    }

    public SWCData fromTmNeuron(TmNeuron neuron, int downsampleModulo) {
        return fromTmNeuron(neuron, neuronCenterOfMass(neuron), downsampleModulo);
    }

    public SWCData fromTmNeuron(TmNeuron neuron, Vec3 externalizedCenter, int downsampleModulo) {

        List<String> headerList = new ArrayList<>();

        double xcenter = externalizedCenter.getX();
        double ycenter = externalizedCenter.getY();
        double zcenter = externalizedCenter.getZ();
        List<SWCNode> nodeList;
        if (downsampleModulo == 0 ) { //|| neuron.getAnchoredPathMap().isEmpty()) {
            nodeList = nodesFromSubtrees(neuron, xcenter, ycenter, zcenter);
        }        
        else {
            nodeList = nodesFromCombinedPath(neuron, xcenter, ycenter, zcenter, downsampleModulo);
        }
        
        // headers: I'm not going to put in all the fields I
        //  saw in the "specification" unless I have to; we only
        //  use the OFFSET field
        headerList.add("# ORIGINAL_SOURCE Janelia Workstation Large Volume Viewer");
        double[] externalPoint =
            new double[] {
                xcenter, ycenter, zcenter
            };
        headerList.add(String.format("# OFFSET %f %f %f", externalPoint[0], externalPoint[1], externalPoint[2]));

        return new SWCData(nodeList, headerList);
    }

    public SWCData fromTmNeuron(List<TmNeuron> neuronList, int downsampleModulo) {
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

    public Vec3 neuronCenterOfMass(TmNeuron neuron) {
        // it is probably not strictly correct, but I'm going
        //  to return the offset for center if the neuron
        //  is null or has no points
        double[] rtnVal = new double[] { 0.0, 0.0, 0.0 };

        if (neuron == null) {
            return calcDefaultCenterOfMass(rtnVal);
        }

        double length = neuron.getGeoAnnotationMap().size();
        if (length == 0) {
            return calcDefaultCenterOfMass(rtnVal);
        }

        //NOTE: the code below assumes that the center-of-mass calculation
        // is made against external-going data.  That is, a conversion from
        // the internal to the external representation of points will be made.
        for (TmGeoAnnotation ann: neuron.getGeoAnnotationMap().values()) {
            double[] externalCoords = 
                    exchanger.getExternal(
                            new double[]{ann.getX(), ann.getY(), ann.getZ()}
                    );
            for (int i = 0; i < externalCoords.length; i++) {
                // Dividing each sum-contribution by length to avoid overlarge numbers.
                rtnVal[i] += externalCoords[i] / length;
            }
        }
        return new Vec3(rtnVal[0], rtnVal[1], rtnVal[2]);
    }

    public Vec3 neuronCenterOfMass(List<TmNeuron> neuronList) {

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

    private List<SWCNode> nodesFromCombinedPath(TmNeuron neuron, double xcenter, double ycenter, double zcenter,
        int downsampleModulo) {

        List<SWCNode> nodeList = new ArrayList<>();
        
        // Find the links back to the auto-generated points lists, from the
        // manual-added points.
        Map<TmAnchoredPathEndpoints, TmAnchoredPath> anchoredPathMap = neuron.getAnchoredPathMap();
        Map<Long,TmAnchoredPathEndpoints> startToEndPoints = new HashMap<>();
        Map<Long,Integer> subAnnIdToIndex = new HashMap<>();
        for (TmAnchoredPathEndpoints endPoints : anchoredPathMap.keySet()) {
            startToEndPoints.put(endPoints.getAnnotationID2(), endPoints);
        }

        int currentIndex = 1;
        int parentIndex = -1;
        
        for (TmGeoAnnotation annotation : neuron.getRootAnnotations()) {
            for (TmGeoAnnotation subAnn : neuron.getSubTreeList(annotation)) {
                // do any traced paths end at this annotation?  if so, trace that
                //  path to this point; note this point is therefore not a root point
                if ( startToEndPoints.get( subAnn.getId() ) != null ) {
                    final TmAnchoredPathEndpoints endpoints = startToEndPoints.get( subAnn.getId() );
                    // Make a node for each path member.
                    TmAnchoredPath anchoredPath = anchoredPathMap.get( endpoints );
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
                        nodeList.add(autoNode);
                        parentIndex = currentIndex;
                        currentIndex++;
                    }

                }
                else {
                    // no paths end at this point; if it's a root, parent index is -1;
                    //  if not, get it from the map
                    if (subAnn.isRoot()) {
                        parentIndex = -1;
                    } else {
                        // not sure why Les added a null check here (should never be missing),
                        //  but I'll leave it in; I'll also make the fallback to a root
                        //  again, rather than the previous parentIndex used (which is
                        //  most likely wrong)
                        Integer parentIndexPutative = subAnnIdToIndex.get(subAnn.getParentId());
                        if (parentIndexPutative != null) {
                            parentIndex = parentIndexPutative;
                        } else {
                            parentIndex = -1;
                        }
                    }
                }
                
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

    private List<SWCNode> nodesFromSubtrees(TmNeuron neuron, double xcenter, double ycenter, double zcenter) {
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

    private Vec3 calcDefaultCenterOfMass(double[] rtnVal) {
        double[] defaultCoords = exchanger.getExternal(rtnVal);
        Vec3 defaultValue = new Vec3(
                defaultCoords[0], defaultCoords[1], defaultCoords[2]
        );
        return defaultValue;
    }

    /**
     * Factory method to encapsulate how to make a node, including the details
     * of relative position.
     * @return properly spec'd node.
     */
    private SWCNode createSWCNode(
            int currentIndex,
            SWCNode.SegmentType segmentType,
            double xAnno,
            double yAnno,
            double zAnno,
            double xcenter,
            double ycenter,
            double zcenter,
            int parentIndex) {
        double[] externalArr = null;
        if (exchanger != null) {
            externalArr = exchanger.getExternal(
                    new double[]{
                        xAnno,
                        yAnno,
                        zAnno,
                    }
            );
        }
        else {
            externalArr = new double[] {
                        xAnno,
                        yAnno,
                        zAnno,
            };
        }
        dumpAtSwc(externalArr, xcenter, ycenter, zcenter);
        return new SWCNode(
                currentIndex,
                segmentType,
                externalArr[0] - xcenter,
                externalArr[1] - ycenter,
                externalArr[2] - zcenter,
                1.0,    // radius, which we don't have right now
                parentIndex
        );
    }

    private void dumpAtSwc(double[] externalArr, double xcenter, double ycenter, double zcenter) {
        System.out.println("Computed center is " + xcenter + "," + ycenter + "," + zcenter);
        System.out.println("Represents: " + externalArr[0] + "," + externalArr[1] + "," + externalArr[2]);
        System.out.println("Node created: " + (externalArr[0]-xcenter) + "," + (externalArr[1]-ycenter) + "," + (externalArr[2]-zcenter));
    }
}
