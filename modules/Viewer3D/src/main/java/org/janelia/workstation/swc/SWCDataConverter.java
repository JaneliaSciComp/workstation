package org.janelia.workstation.swc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.model.domain.tiledMicroscope.TmAnchoredPath;
import org.janelia.model.domain.tiledMicroscope.TmAnchoredPathEndpoints;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class handles the details of translating TmNeurons into SWCData
 */
public class SWCDataConverter {

    private static final Logger log = LoggerFactory.getLogger(SWCDataConverter.class);
    
    private static final int SWC_X = 0;
    private static final int SWC_Y = 1;
    private static final int SWC_Z = 2;

    // SWC data can't have a null radius, so we have to choose something;
    //  1.0 is about as innocuous a value as you can choose; plus, we
    //  were already using it implicitly before we handled radii explicitly
    // we have a potentially different radius for automatically traced paths, too
    private static final double DEFAULT_POINT_RADIUS = 1.0;
    private static final double DEFAULT_PATH_RADIUS = 1.0;

    private ImportExportSWCExchanger exchanger;

    public ImportExportSWCExchanger getExchanger() {
        return exchanger;
    }

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

    public SWCData fromTmNeuron(TmNeuronMetadata neuron) {
        return fromTmNeuron(neuron, neuronCenterOfMass(neuron), 1);
    }

    public SWCData fromTmNeuron(TmNeuronMetadata neuron, int downsampleModulo) {
        return fromTmNeuron(neuron, neuronCenterOfMass(neuron), downsampleModulo);
    }

    public SWCData fromTmNeuron(TmNeuronMetadata neuron, double[] externalizedCenter, int downsampleModulo) {

        List<String> headerList = new ArrayList<>();

        double xcenter = externalizedCenter[SWC_X];
        double ycenter = externalizedCenter[SWC_Y];
        double zcenter = externalizedCenter[SWC_Z];
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

        return new SWCData(nodeList, headerList, externalizedCenter);
    }

    public List<SWCData> fromTmNeuron(Collection<TmNeuronMetadata> neuronList, Map<Long,List<String>> extraHeaders, int downsampleModulo) {
        List<SWCData> rtnList = new ArrayList<>();
        double[] com = neuronCenterOfMass(neuronList);

        List<SWCData> dataList = new ArrayList<>();
        for (TmNeuronMetadata neuron : neuronList) {
            if (neuron != null && neuron.getGeoAnnotationMap().size() > 0) {
                log.debug("Creating SWCData for neuron{} (id={})",neuron.getName(),neuron.getId());
                final SWCData neuronData = fromTmNeuron(neuron, com, downsampleModulo);
                List<String> headers = extraHeaders.get( neuron.getId() );
                for (String header: headers) {
                    if (! header.startsWith("#")) {
                        header = "# " + header;
                    }
                    neuronData.getHeaderList().add(header);
                }
                dataList.add(neuronData);
            }
            else {
                log.trace("Skipping empty neuron "+neuron);
            }
        }

        // if more than one in list, merge everything into first one
        if (dataList.isEmpty()) {
            return null;
        } 
        else if (dataList.size() == 1) {
            rtnList.add(dataList.get(0));
        } 
        else {
            for (int i=0; i < dataList.size(); i++) {
                rtnList.add(dataList.get(i));
            }
        }
        return rtnList;
    }

    public SWCData fromAllTmNeuron(Collection<TmNeuronMetadata> neuronList, int downsampleModulo) {
        double[] com = neuronCenterOfMass(neuronList);

        List<SWCData> dataList = new ArrayList<>();
        for (TmNeuronMetadata neuron: neuronList) {
            if (neuron != null && neuron.getGeoAnnotationMap().size() > 0) {
                dataList.add(fromTmNeuron(neuron, com, downsampleModulo));
            }
            else {
                log.trace("Skipping empty neuron "+neuron);
            }
        }

        // if more than one in list, merge everything into first one
        if (dataList.size() == 0) {
            return null;
        }
        else if (dataList.size() == 1) {
            return dataList.get(0);
        } 
        else {
            // merge all to first
            for (int i=1; i < dataList.size(); i++) {
                dataList.get(0).addDataFrom(dataList.get(i));
            }
            return dataList.get(0);
        }
    }

    public double[] neuronCenterOfMass(TmNeuronMetadata neuron) {
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
        return rtnVal;
    }

    public double[] neuronCenterOfMass(Collection<TmNeuronMetadata> neuronList) {

        double[] com;
        double[] sum = new double[3];

        int nNodes;
        int totalNodes = 0;

        for (TmNeuronMetadata neuron: neuronList) {
            if (neuron == null) {
                continue;
            }
            nNodes = neuron.getGeoAnnotationMap().size();
            if (nNodes == 0) {
                continue;
            }
            com = neuronCenterOfMass(neuron);
            for (int i = 0; i < com.length; i++) {
                com[i] *= nNodes;
            }
            for (int i = 0; i < sum.length; i++) {
                sum[i] += com[i];
            }
            totalNodes += nNodes;
        }

        if (totalNodes > 0) {
            final double totalNodesReciprocal = 1.0 / totalNodes;
            for (int i = 0; i < sum.length; i++) {
                sum[i] *= totalNodesReciprocal;
            }
        }
        // note that if totalNodes is zero, sum is also all zeros
        return sum;
    }

    private List<SWCNode> nodesFromCombinedPath(TmNeuronMetadata neuron, double xcenter, double ycenter, double zcenter,
        int downsampleModulo) {

        List<SWCNode> nodeList = new ArrayList<>();

        // Find the links back to the auto-generated points lists, from the
        // manual-added points.
        Map<TmAnchoredPathEndpoints, TmAnchoredPath> anchoredPathMap = neuron.getAnchoredPathMap();
        Map<Long,TmAnchoredPathEndpoints> startToEndPoints = new HashMap<>();
        Map<Long,Integer> subAnnIdToIndex = new HashMap<>();
        for (TmAnchoredPathEndpoints endPoints : anchoredPathMap.keySet()) {
            startToEndPoints.put(endPoints.getSecondAnnotationID(), endPoints);
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
                    if ( subAnnIdToIndex.get( endpoints.getFirstAnnotationID() ) != null ) {
                        parentIndex = subAnnIdToIndex.get( endpoints.getFirstAnnotationID() );
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
                                DEFAULT_PATH_RADIUS,
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
                double radius = DEFAULT_POINT_RADIUS;
                if (subAnn.getRadius() != null) {
                    radius = subAnn.getRadius();
                }
                SWCNode manualNode = createSWCNode(
                        currentIndex++,
                        getSegmentType(subAnn),
                        subAnn.getX(),
                        subAnn.getY(),
                        subAnn.getZ(),
                        xcenter,
                        ycenter,
                        zcenter,
                        radius,
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

    private List<SWCNode> nodesFromSubtrees(TmNeuronMetadata neuron, double xcenter, double ycenter, double zcenter) {
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

                double radius = DEFAULT_POINT_RADIUS;
                if (ann.getRadius() != null) {
                    radius = ann.getRadius();
                }
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
                                radius,
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

    private double[] calcDefaultCenterOfMass(double[] rtnVal) {
        double[] defaultCoords = exchanger.getExternal(rtnVal);
        return defaultCoords;
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
            double radius,
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
        
        log.trace("SWC Node {}",currentIndex);
        log.trace("  Computed center is {},{},{}", xcenter, ycenter, zcenter);
        log.trace("  Represents: {},{},{}", externalArr[0], externalArr[1], externalArr[2]);
        log.trace("  Node created: {},{},{}", (externalArr[0]-xcenter), (externalArr[1]-ycenter), (externalArr[2]-zcenter));
        
        return new SWCNode(
                currentIndex,
                segmentType,
                externalArr[0] - xcenter,
                externalArr[1] - ycenter,
                externalArr[2] - zcenter,
                radius,
                parentIndex
        );
    }
}
