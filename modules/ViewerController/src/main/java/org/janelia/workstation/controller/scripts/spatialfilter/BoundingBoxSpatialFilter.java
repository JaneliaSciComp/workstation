package org.janelia.workstation.controller.scripts.spatialfilter;

import org.janelia.model.domain.tiledMicroscope.BoundingBox3d;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BoundingBoxSpatialFilter implements NeuronSpatialFilter {
    BoundingBoxSpatialIndex index;
    BoundingBox3d currentRegion;
    HashMap<Long, BoundingBox3d> boxes;
    Set<Long> fragments;
    Set<Long> userNeuronIds;
    int numTotalNeurons;
    private static final Logger log = LoggerFactory.getLogger(BoundingBoxSpatialFilter.class);

    private double distance = 450; // distance from the neuron to include for proximity

    @Override
    // return a union of all individual proximities
    public synchronized Set<Long> filterNeurons() {
        Set<Long> neuronFrags = new HashSet<>();
        neuronFrags.addAll(fragments);
        neuronFrags.addAll(userNeuronIds);
        return neuronFrags;
    }

    @Override
    public void initFilter(Collection<BoundingBox3d> boundingBoxes,
                           Collection<TmNeuronMetadata> neurons) {
        log.info("Starting to build spatial filter");
        numTotalNeurons = boundingBoxes.size() + neurons.size();
        // load all the neuron points into the index
        index = new BoundingBoxSpatialIndex();
        userNeuronIds = new HashSet<>();
        fragments = new HashSet<>();
        int count = 0;
        for (TmNeuronMetadata neuron: neurons) {
            userNeuronIds.add(neuron.getId());
        }

        boxes = new HashMap<>();
        for (BoundingBox3d boundingBox: boundingBoxes) {
            count++;
            index.addToIndex(boundingBox);
            boxes.put(boundingBox.getDomainId(), boundingBox);
        }
        log.info("Finished building spatial filter");
    }

    @Override
    // return total number of neurons (unfiltered)
    public int getNumTotalNeurons() {
        return numTotalNeurons;
    }

    public String getLabel() {
        return "Neuron Selection Filter";
    }

    private BoundingBox3d calcBoundingBox (TmGeoAnnotation vertex) {
        double[] min = new double[]{vertex.getX()-distance,vertex.getY()-distance,vertex.getZ()-distance};
        double[] max = new double[]{vertex.getX()+distance,vertex.getY()+distance,vertex.getZ()+distance};
        BoundingBox3d box = new BoundingBox3d(min, max);
        return box;
    }

    @Override
    // remove bounding box and fragments in vicinity
    public synchronized NeuronUpdates deleteNeuron(TmNeuronMetadata neuron) {
        if (!neuron.isFragment())
            userNeuronIds.remove(neuron.getId());
        else {
            if (boxes.containsKey(neuron.getId())) {
                index.removeFromIndex(boxes.get(neuron.getId()));
            }
        }
        Set<Long> delSet = new HashSet<>();
        delSet.add(neuron.getId());
        NeuronUpdates updates = new NeuronUpdates();
        updates.setDeletedNeurons(delSet);
        return updates;
    }

    @Override
    public synchronized NeuronUpdates addNeuron(TmNeuronMetadata neuron) {
        if (!neuron.isFragment())
            userNeuronIds.add(neuron.getId());
        else {
            if (boxes.containsKey(neuron.getId())) {
                index.removeFromIndex(boxes.get(neuron.getId()));
            }
        }
        return new NeuronUpdates();
    }

    @Override
    public synchronized NeuronUpdates updateNeuron(TmNeuronMetadata neuron) {
        userNeuronIds.add(neuron.getId());
        fragments.remove(neuron.getId());
        if (boxes.containsKey(neuron.getId())) {
            index.removeFromIndex(boxes.get(neuron.getId()));
        }
        return new NeuronUpdates();
    }

    public synchronized NeuronUpdates selectVertex(TmGeoAnnotation annotation) {
        currentRegion = calcBoundingBox(annotation);
        double[] boxMin = new double[]{currentRegion.getMinX(),currentRegion.getMinY(),currentRegion.getMinZ()};
        double[] boxMax = new double[]{currentRegion.getMaxX(),currentRegion.getMaxY(),currentRegion.getMaxZ()};
        Set<Long> newFrags = index.getFragmentIdsInBoundingBox(boxMin, boxMax);
        Iterator<Long> newFragIter = newFrags.iterator();
        Iterator<Long> oldFragIter = fragments.iterator();
        Set<Long> addSet = new HashSet<>();
        Set<Long> delSet = new HashSet<>();
        while (newFragIter.hasNext()) {
            Long frag = newFragIter.next();
            if (!fragments.contains(frag) && !userNeuronIds.contains(frag))
                addSet.add(frag);
        }        
        fragments = newFrags;
        while (oldFragIter.hasNext()) {
            Long frag = oldFragIter.next();
            if (!fragments.contains(frag) && !userNeuronIds.contains(frag))
                delSet.add(frag);
        }

        NeuronUpdates updates = new NeuronUpdates();
        updates.setAddedNeurons(addSet);
        updates.setDeletedNeurons(delSet);
        return updates;
    }

    @Override
    public void clearFilter() {
        index.clear();
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
    
    public Map<String,Object> getFilterOptions() {
        Map<String,Object> options = new HashMap<>();
        options.put("distance",Double.class);
        return options;
    }

    public Map<String, String> getFilterOptionsUnits() {
        Map<String,String> options = new HashMap<>();
        options.put("distance", "µm");
        return options;
    }

    public void setFilterOptions(Map<String, Object> filterOptions) {
        distance = ((Double)filterOptions.get("distance"));
    }
}
