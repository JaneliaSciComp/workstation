package org.janelia.it.workstation.shared.util;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.workstation.geom.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * this class handles the details of translating TmNeurons into SWCData
 */
public class SWCDataConverter {

    public static SWCData fromTmNeuron(TmNeuron neuron) {
        return fromTmNeuron(neuron, neuronCenterOfMass(neuron));
    }

    public static SWCData fromTmNeuron(TmNeuron neuron, Vec3 center) {

        List<SWCNode> nodeList = new ArrayList<SWCNode>();
        List<String> headerList = new ArrayList<String>();

        // map the annotation IDs to the indices to be used in the swc file
        Map<Long, Integer> annMap = new HashMap<Long, Integer>();

        double xcenter = center.getX();
        double ycenter = center.getY();
        double zcenter = center.getZ();

        int currentIndex = 1;
        int segmentType;
        int parentIndex;
        for (TmGeoAnnotation root: neuron.getRootAnnotations()) {
            for (TmGeoAnnotation ann: neuron.getSubTreeList(root)) {
                if (ann.isRoot()) {
                    parentIndex = -1;
                } else {
                    parentIndex = annMap.get(ann.getParentId());
                }

                // only marking "fork" and "end, as that's
                //  all we can surmise from geometry
                if (ann.getChildIds().size() == 0) {
                    segmentType = 6;
                } else if (ann.getChildIds().size() > 1) {
                    segmentType = 5;
                } else {
                    segmentType = 0;
                }

                nodeList.add(new SWCNode(
                    currentIndex,
                    segmentType,
                    ann.getX() - xcenter,
                    ann.getY() - ycenter,
                    ann.getZ() - zcenter,
                    1.0,    // radius, which we don't have right now
                    parentIndex
                ));
                annMap.put(ann.getId(), currentIndex);
                currentIndex++;
            }
        }

        // headers: I'm not going to put in all the fields I
        //  saw in the "specification" unless I have to; we only
        //  use the OFFSET field
        headerList.add("# ORIGINAL_SOURCE Janelia Workstation Large Volume Viewer");
        headerList.add(String.format("# OFFSET %f %f %f", xcenter, ycenter, zcenter));

        return new SWCData(nodeList, headerList);
    }

    public static SWCData fromTmNeuron(List<TmNeuron> neuronList) {
        Vec3 com = neuronCenterOfMass(neuronList);

        List<SWCData> dataList = new ArrayList<SWCData>();
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

}
