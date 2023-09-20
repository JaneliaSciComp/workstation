package org.janelia.workstation.controller.scripts.spatialfilter;

import com.github.davidmoten.rtreemulti.Entry;
import com.github.davidmoten.rtreemulti.RTree;
import com.github.davidmoten.rtreemulti.geometry.Rectangle;
import edu.wlu.cs.levy.CG.KeySizeException;
import org.janelia.model.domain.tiledMicroscope.BoundingBox3d;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Spatial index for doing range searches to find fragments that are within a certain distance
 * from other neurons
 *
 * @author David Schauder
 * @author <a href="mailto:schauderd@janelia.hhmi.org">David Schauder</a>
 */
public class BoundingBoxSpatialIndex {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    RTree<Long, Rectangle> tree;

    public BoundingBoxSpatialIndex() {
        this.tree = RTree.star().dimensions(3).maxChildren(6).create();
        log.trace("Creating spatial index");
    }

    /**
     * Returns all FragmentIds  with the bounding box
     * @param minXYZ minimums for the bounding box
     * @param maxXYZ maximums for the bounding box
     * @return
     */
    public Set<Long> getFragmentIdsInBoundingBox(double[] minXYZ, double[] maxXYZ) {
        Set<Long> fragments = new HashSet<>();
        Iterable<Entry<Long, Rectangle>> results =
                tree.search(Rectangle.create(minXYZ, maxXYZ));
        Iterator<Entry<Long, Rectangle>> iter = results.iterator();
        while (iter.hasNext()) {
            fragments.add(iter.next().value());
        }
        return fragments;
    }

    public NeuronUpdates selectVertex(TmGeoAnnotation annotation) {
        return new NeuronUpdates();
    }

    public boolean addToIndex(BoundingBox3d box) {
        Long domainId = box.getDomainId();
        double[] mins = new double[]{box.getMinX(), box.getMinY(), box.getMinZ()};
        double[] maxs = new double[]{box.getMaxX(), box.getMaxY(), box.getMaxZ()};
        tree = tree.add(domainId, Rectangle.create(mins, maxs));

        return true;
    }

    public boolean removeFromIndex(BoundingBox3d box) {
        Long domainId = box.getDomainId();
        double[] mins = new double[]{box.getMinX(), box.getMinY(), box.getMinZ()};
        double[] maxs = new double[]{box.getMaxX(), box.getMaxY(), box.getMaxZ()};
        tree = tree.delete(domainId, Rectangle.create(mins, maxs));
        return true;
    }

    public boolean isValid() {
        return true;
    }
    
    public void clear() {
        this.tree = RTree.star().dimensions(3).maxChildren(6).create();
    }
}
