package org.janelia.workstation.controller.scripts.spatialfilter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeyMissingException;
import edu.wlu.cs.levy.CG.KeySizeException;
import org.janelia.model.domain.tiledMicroscope.BoundingBox3d;
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

    private KDTree<Long> index;

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
            List<Long> points = index.range(minXYZ, maxXYZ);
            Set<Long> fragments = new HashSet<>();
            if (points!=null && points.size()>0) {
                for (Long point: points) {
                    fragments.add(point);
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

    public boolean addToIndex(BoundingBox3d box) {
        try {
            Long domainId = box.getDomainId();
            double[] corner1 = new double[]{box.getMinX(), box.getMinY(), box.getMinZ()};
            double[] corner2 = new double[]{box.getMinX(), box.getMaxY(), box.getMinZ()};
            double[] corner3 = new double[]{box.getMinX(), box.getMaxY(), box.getMaxZ()};
            double[] corner4 = new double[]{box.getMaxX(), box.getMinY(), box.getMinZ()};
            double[] corner5 = new double[]{box.getMaxX(), box.getMaxY(), box.getMinZ()};
            double[] corner6 = new double[]{box.getMaxX(), box.getMaxY(), box.getMaxZ()};
            index.insert(corner1, domainId);
            index.insert(corner2, domainId);
            index.insert(corner3, domainId);
            index.insert(corner4, domainId);
            index.insert(corner5, domainId);
            index.insert(corner6, domainId);
        }
        catch (KeySizeException | KeyDuplicateException ex) {
            return false;
        }
        return true;
    }

    public boolean removeFromIndex(BoundingBox3d box) {
        try {
            double[] corner1 = new double[]{box.getMinX(), box.getMinY(), box.getMinZ()};
            double[] corner2 = new double[]{box.getMinX(), box.getMaxY(), box.getMinZ()};
            double[] corner3 = new double[]{box.getMinX(), box.getMaxY(), box.getMaxZ()};
            double[] corner4 = new double[]{box.getMaxX(), box.getMinY(), box.getMinZ()};
            double[] corner5 = new double[]{box.getMaxX(), box.getMaxY(), box.getMinZ()};
            double[] corner6 = new double[]{box.getMaxX(), box.getMaxY(), box.getMaxZ()};
            index.delete(corner1);
            index.delete(corner2);
            index.delete(corner3);
            index.delete(corner4);
            index.delete(corner5);
            index.delete(corner6);
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
