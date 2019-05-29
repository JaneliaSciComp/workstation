package org.janelia.workstation.gui.large_volume_viewer.annotation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.viewer3d.BoundingBox3d;
import org.janelia.workstation.gui.large_volume_viewer.neuron_api.NeuronProximitySpatialIndex;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.workstation.core.util.ConsoleProperties;

public class NeuronSelectionSpatialFilter implements NeuronSpatialFilter {
    NeuronProximitySpatialIndex index;
    BoundingBox3d currentRegion;
    Set<Long> fragments = new HashSet<>();
    Set<Long> userNeuronIds;
    int numTotalNeurons;

    private double distance = 450; // distance from the neuron to include for proximity

    @Override
    // return a union of all individual proximities
    public Set<Long> filterNeurons() {
        Set<Long> neuronFrags = new HashSet<>();
        neuronFrags.addAll(fragments);
        neuronFrags.addAll(userNeuronIds);
        return neuronFrags;
    }

    @Override
    public void initFilter(Collection<TmNeuronMetadata> neuronList) {
        numTotalNeurons = neuronList.size();
        // load all the neuron points into the index
        index = new NeuronProximitySpatialIndex();
        userNeuronIds = new HashSet<>();
        for (TmNeuronMetadata neuron: neuronList) {
            if (!neuron.getOwnerKey().equals(ConsoleProperties.getInstance()
                    .getProperty("console.LVVHorta.tracersgroup").trim())) {
                userNeuronIds.add(neuron.getId());
            } else {
                index.addToIndex(neuron);
            }
        }
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
        return new NeuronUpdates();
    }

    @Override
    // calculate single bounding box and return fragments in vicinity
    public synchronized NeuronUpdates addNeuron(TmNeuronMetadata neuron) {
        userNeuronIds.add(neuron.getId());
        return new NeuronUpdates();
    }

    @Override
    public synchronized NeuronUpdates updateNeuron(TmNeuronMetadata neuron) {
        userNeuronIds.add(neuron.getId());
        fragments.remove(neuron.getId());
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
            if (!fragments.contains(frag))
                addSet.add(frag);
        }        
        fragments = newFrags;
        while (oldFragIter.hasNext()) {
            Long frag = oldFragIter.next();
            if (!fragments.contains(frag))
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
