package org.janelia.workstation.controller.scripts.spatialfilter;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.geom.BoundingBox3d;
import org.janelia.workstation.geom.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeuronSelectionSpatialFilter implements NeuronSpatialFilter {
    NeuronProximitySpatialIndex index;
    BoundingBox3d currentRegion;
    Set<Long> fragments = new HashSet<>();
    Set<Long> userNeuronIds;
    int numTotalNeurons;
    private static final Logger log = LoggerFactory.getLogger(NeuronSelectionSpatialFilter.class);

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
    public void initFilter(Collection<TmNeuronMetadata> neuronList) {
        log.info("Starting to build spatial filter");
        numTotalNeurons = neuronList.size();
        // load all the neuron points into the index
        index = new NeuronProximitySpatialIndex();
        userNeuronIds = new HashSet<>();
        String systemGroup = ConsoleProperties.getInstance()
                .getProperty("console.LVVHorta.tracersgroup").trim();
        int count = 0;
        for (TmNeuronMetadata neuron: neuronList) {
            count++;
            if (!neuron.getOwnerKey().equals(systemGroup)) {
                userNeuronIds.add(neuron.getId());
            } else {
                index.addToIndex(neuron);
            }
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
        Vec3 min = new Vec3(vertex.getX()-distance,vertex.getY()-distance,vertex.getZ()-distance);
        Vec3 max = new Vec3(vertex.getX()+distance,vertex.getY()+distance,vertex.getZ()+distance);
        BoundingBox3d box = new BoundingBox3d();
        box.setMin(min);
        box.setMax(max);
        return box;
    }

    @Override
    // remove bounding box and fragments in vicinity
    public synchronized NeuronUpdates deleteNeuron(TmNeuronMetadata neuron) {
        userNeuronIds.remove(neuron.getId());        
        fragments.remove(neuron.getId());
        index.removeFromIndex(neuron);
        Set<Long> delSet = new HashSet<>();
        delSet.add(neuron.getId());
        NeuronUpdates updates = new NeuronUpdates();
        updates.setDeletedNeurons(delSet);
        return updates;
    }

    @Override
    public synchronized NeuronUpdates addNeuron(TmNeuronMetadata neuron) {
        userNeuronIds.add(neuron.getId());
        fragments.remove(neuron.getId());
        return new NeuronUpdates();
    }

    @Override
    public synchronized NeuronUpdates updateNeuron(TmNeuronMetadata neuron) {
        userNeuronIds.add(neuron.getId());
        fragments.remove(neuron.getId());
        index.removeFromIndex(neuron);
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
