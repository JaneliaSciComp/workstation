package org.janelia.workstation.controller.spatialfilter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeyMissingException;
import edu.wlu.cs.levy.CG.KeySizeException;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spatial index for doing range searches to find fragments that are within a certain distance
 * from other neurons
 *
 * @author David Schauder
 * @author <a href="mailto:schauderd@janelia.hhmi.org">David Schauder</a>
 */
public class NeuronProximitySpatialIndex {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private KDTree<TmGeoAnnotation> index;

    // Is the index currently in a valid, usable state?
    private AtomicBoolean valid = new AtomicBoolean(false);


    public NeuronProximitySpatialIndex() {
        this.index = new KDTree<>(3);
        log.trace("Creating spatial index");
    }

    /**
     * Returns all TmGeoAnnotations within the bounding box
     * @param minXYZ minimums for the bounding box
     * @param maxXYZ maximums for the bounding box
     * @return
     */
    public Set<Long> getFragmentIdsInBoundingBox(double[] minXYZ, double[] maxXYZ) {
        if (index==null) return null;
        try {
            List<TmGeoAnnotation> points = index.range(minXYZ, maxXYZ);
            Set<Long> fragments = new HashSet<>();
            if (points!=null && points.size()>0) {
                for (TmGeoAnnotation point: points) {
                    fragments.add(point.getNeuronId());
                }
            }
            return fragments;
        } 
        catch (KeySizeException ex) {
            log.warn("Exception while finding fragments in spatial index", ex);
            return null;
        }
    }

    public NeuronUpdates selectVertex(TmGeoAnnotation annotation) {
        return new NeuronUpdates();
    }

    public boolean addToIndex(TmNeuronMetadata neuron) {
        try {
            for (TmGeoAnnotation annotation: neuron.getGeoAnnotationMap().values()) {
                double[] key = new double[]{annotation.getX(), annotation.getY(), annotation.getZ()};
                index.insert(key, annotation);
            }
        }
        catch (KeySizeException | KeyDuplicateException ex) {
            return false;
        }
        return true;
    }

    public boolean removeFromIndex(TmNeuronMetadata neuron) {
        try {
            for (TmGeoAnnotation annotation: neuron.getGeoAnnotationMap().values()) {
                double[] key = new double[]{annotation.getX(), annotation.getY(), annotation.getZ()};
                index.delete(key);
            }
        }
        catch (KeySizeException | KeyMissingException ex) {
            return false;
        }
        return true;
    }

    public boolean isValid() {
        return valid.get();
    }
    
    public void clear() {
        this.index = new KDTree<>(3);
    }
}
