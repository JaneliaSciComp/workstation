package org.janelia.workstation.gui.large_volume_viewer.annotation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;

import com.google.common.collect.Sets;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.viewer3d.BoundingBox3d;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.gui.large_volume_viewer.neuron_api.NeuronProximitySpatialIndex;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

public class NeuronProximitySpatialFilter implements NeuronSpatialFilter {
    NeuronProximitySpatialIndex index;
    Map<Long, BoundingBox3d> spatialRegions;
    Map<Long, Set<Long>> proximalFragments;
    Set<Long> userNeuronIds;
    boolean upToDate = false;
    int numTotalNeurons;

    private double distance = 200; // distance from the neuron to include for proximity

    @Override
    // return a union of all individual proximities
    public synchronized Set<Long> filterNeurons() {
        Set<Long> neuronFrags = new HashSet<>();
        for (Set<Long> fragSet: proximalFragments.values()) {
            neuronFrags.addAll(fragSet);
        }
        neuronFrags.addAll(userNeuronIds);
        return neuronFrags;
    }

    @Override
    public void initFilter(Collection<TmNeuronMetadata> neuronList) {
        numTotalNeurons = neuronList.size();
        // load all the neuron points into the index
        index = new NeuronProximitySpatialIndex();
        String systemOwnerKey= ConsoleProperties.getInstance().getProperty("console.LVVHorta.tracersgroup").trim();
        userNeuronIds = new HashSet<>();
        List<TmNeuronMetadata> userNeurons = new ArrayList<>();
        for (TmNeuronMetadata neuron: neuronList) {
            index.addToIndex(neuron);
            if (!neuron.getOwnerKey().equals(systemOwnerKey)) {
                userNeurons.add(neuron);
            }
        }

        // calculate the bounding boxes for all the user's neurons
        // and perform a range search on all relevant bounding boxes
        spatialRegions = new HashMap<>();
        proximalFragments = new HashMap<>();
        if (userNeurons.size()>0) {
            for (TmNeuronMetadata neuron : userNeurons) {
                userNeuronIds.add(neuron.getId());
                BoundingBox3d box = calcBoundingBox(neuron);
                spatialRegions.put(neuron.getId(),box);
                double[] boxMin = new double[]{box.getMinX(),box.getMinY(),box.getMinZ()};
                double[] boxMax = new double[]{box.getMaxX(),box.getMaxY(),box.getMaxZ()};
                Set<Long> frags = index.getFragmentIdsInBoundingBox(boxMin, boxMax);
                if (frags!=null) {
                    proximalFragments.put(neuron.getId(),frags);
                }
            }
        }
    }

    @Override
    // return total number of neurons (unfiltered)
    public int getNumTotalNeurons() {
        return numTotalNeurons;
    }

    public String getLabel() {
        return "Neuron Proximity Filter";
    }

    private BoundingBox3d calcBoundingBox (TmNeuronMetadata neuron) {
        Vec3 min = new Vec3(1000000,1000000,1000000);
        Vec3 max = new Vec3(0,0,0);
        Iterator<TmGeoAnnotation> iter = neuron.getGeoAnnotationMap().values().iterator();
        while (iter.hasNext()) {
            TmGeoAnnotation point = iter.next();
            if (min.getX()>point.getX())
                min.setX(point.getX());
            if (max.getX()<point.getX())
                max.setX(point.getX());
            if (min.getY()>point.getY())
                min.setY(point.getY());
            if (max.getY()<point.getY())
                max.setY(point.getY());
            if (min.getZ()>point.getZ())
                min.setZ(point.getZ());
            if (max.getZ()<point.getZ())
                max.setZ(point.getZ());
        }
        // add distance as a buffer
        min.setX(min.getX()-distance);
        min.setY(min.getY()-distance);
        min.setZ(min.getZ()-distance);
        max.setX(max.getX()+distance);
        max.setY(max.getY()+distance);
        max.setZ(max.getZ()+distance);
        BoundingBox3d box = new BoundingBox3d();
        box.setMin(min);
        box.setMax(max);
        return box;
    }

    @Override
    // remove bounding box and fragments in vicinity
    public synchronized NeuronUpdates deleteNeuron(TmNeuronMetadata neuron) {
        userNeuronIds.remove(neuron.getId());
        NeuronUpdates updates = new NeuronUpdates();
        spatialRegions.remove(neuron.getId());
        Set<Long> neuronFrags = proximalFragments.get(neuron.getId());
        proximalFragments.remove(neuron.getId());

        // check which frags need to removed from the visible list
        if (neuronFrags!=null) {
            Set<Long> totalFrags = filterNeurons();
            Set<Long> deleteFrags = Sets.difference(neuronFrags, totalFrags);
            if (deleteFrags.size()>0)
                updates.setDeletedNeurons(deleteFrags);
        }
        return updates;
    }

    @Override
    // calculate single bounding box and return fragments in vicinity
    public synchronized NeuronUpdates addNeuron(TmNeuronMetadata neuron) {
        userNeuronIds.add(neuron.getId());
        BoundingBox3d box = calcBoundingBox(neuron);       
        spatialRegions.put(neuron.getId(),box);
        double[] boxMin = new double[]{box.getMinX(),box.getMinY(),box.getMinZ()};
        double[] boxMax = new double[]{box.getMaxX(),box.getMaxY(),box.getMaxZ()};
        Set<Long> frags = index.getFragmentIdsInBoundingBox(boxMin, boxMax);
        proximalFragments.put(neuron.getId(), frags);
        NeuronUpdates updates = new NeuronUpdates();
        updates.setAddedNeurons(frags);
        return updates;
    }

    @Override
    // update the bounding box for this neuron
    public synchronized NeuronUpdates updateNeuron(TmNeuronMetadata neuron) {
        userNeuronIds.add(neuron.getId());
        Set<Long> oldFrags = proximalFragments.get(neuron.getId());  
        if (oldFrags==null)
            oldFrags = new HashSet<Long>();
        BoundingBox3d newBox = calcBoundingBox(neuron);
        double[] boxMin = new double[]{newBox.getMinX(),newBox.getMinY(),newBox.getMinZ()};
        double[] boxMax = new double[]{newBox.getMaxX(),newBox.getMaxY(),newBox.getMaxZ()};
        Set<Long> newFrags = index.getFragmentIdsInBoundingBox(boxMin, boxMax);

        Set<Long> totalFrags = filterNeurons();
        spatialRegions.put(neuron.getId(),newBox);
        proximalFragments.put(neuron.getId(),newFrags);
        Iterator<Long> newFragIter = newFrags.iterator();
        Set<Long> addSet = new HashSet<>();
        Set<Long> delSet = new HashSet<>();
        while (newFragIter.hasNext()) {
            Long frag = newFragIter.next();
            if (!totalFrags.contains(frag))
                addSet.add(frag);
        }
        totalFrags = filterNeurons();
        Iterator<Long> oldFragIter = oldFrags.iterator();
        while (oldFragIter.hasNext()) {
            Long frag = oldFragIter.next();
            if (!totalFrags.contains(frag))
                delSet.add(frag);
        }

        NeuronUpdates updates = new NeuronUpdates();
        updates.setAddedNeurons(addSet);
        updates.setDeletedNeurons(delSet);
        return updates;
    }

    @Override
    public NeuronUpdates selectVertex(TmGeoAnnotation annotation) {
        return new NeuronUpdates();
    }

    @Override
    public void clearFilter() {
        index.clear();
        spatialRegions = new HashMap<>();
        proximalFragments = new HashMap<>();
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
