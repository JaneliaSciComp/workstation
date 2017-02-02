package org.janelia.it.workstation.gui.large_volume_viewer.neuron_api;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wlu.cs.levy.CG.Checker;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeyMissingException;
import edu.wlu.cs.levy.CG.KeySizeException;

/**
 * Spatial index for fast access to local NeuronVertexes, given a position in micron space.
 *
 * @author Christopher Bruns
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NeuronVertexSpatialIndex {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Map<NeuronVertex, CacheableKey> cachedKeys = new HashMap<>();
    
    private KDTree<NeuronVertex> index; // Cannot be final because it doesn't have a clear method

    // Is the index currently in a valid, usable state?
    private AtomicBoolean valid = new AtomicBoolean(false);
    
    public NeuronVertexSpatialIndex() {
        log.trace("Creating spatial index");
    }

    /**
     * Returns the anchor that is closest to the location given in microns.
     * @param micronXYZ
     * @return
     */
    public NeuronVertex getAnchorClosestToMicronLocation(double[] micronXYZ) {
        if (index==null) return null;
        List<NeuronVertex> nbrs = getAnchorClosestToMicronLocation(micronXYZ, 1);
        if (nbrs.isEmpty()) return null;
        return nbrs.get(0);
    }
    
    /**
     * Returns the N closest anchors to the location given in microns. The locations are sorted in 
     * order from closest to farthest.
     * @param micronXYZ
     * @param n
     * @return
     */
    public List<NeuronVertex> getAnchorClosestToMicronLocation(double[] micronXYZ, int n) {
        if (index==null) return Collections.emptyList();
        try {
            return index.nearest(micronXYZ, n);
        } 
        catch (KeySizeException ex) {
            log.warn("Exception while finding anchor in spatial index", ex);
            return null;
        }
    }

    /**
     * Returns the N closest anchors to the location given in micron units. The locations are sorted in 
     * order from closest to farthest.
     * @param micronXYZ micron location
     * @param n number of results to return
     * @param filter filter which anchors to exclude
     * @return list of matching anchors
     */
    public List<NeuronVertex> getAnchorClosestToMicronLocation(double[] micronXYZ, int n, final SpatialFilter filter) {
        if (index==null) return Collections.emptyList();
        try {
            return index.nearest(micronXYZ, n, new Checker<NeuronVertex>() {
                @Override
                public boolean usable(NeuronVertex v) {
                    if (v instanceof NeuronVertexAdapter) {
                        TmGeoAnnotation ann = ((NeuronVertexAdapter) v).getTmGeoAnnotation();
                        return filter.include(v, ann);
                    }
                    else {
                        return filter.include(v, null);
                    }
                }
            });
        } 
        catch (KeySizeException ex) {
            log.warn("Exception while finding anchor in spatial index", ex);
            return null;
        }
    }

    /**
     * Returns all the anchors found in the area given by two corners points, given in micron units.  
     * @param p1 lower corner
     * @param p2 higher corner
     * @return list of anchors 
     */
    public List<NeuronVertex> getAnchorsInMicronArea(double[] p1, double[] p2) {
        if (index==null) return Collections.emptyList();
        try {
            log.debug("Finding anchors in area bounded by points: p1=({},{},{}) p2=({},{},{})",p1[0],p1[1],p1[2],p2[0],p2[1],p2[2]);
            return index.range(p1, p2);
        } 
        catch (KeySizeException ex) {
            log.warn("Exception while anchors in area using spatial index", ex);
            return null;
        }
    }
    
    /**
     * Return the location of the vertex in micrometers, for indexing within the KD-tree.
     * @param v
     * @return
     */
    private double[] keyForVertex(NeuronVertex v) {
        if (cachedKeys.containsKey(v)) {
            return cachedKeys.get(v).toArray();
        }
        else {
            float xyz[] = v.getLocation(); // Neuron API returns coordinates in micrometers
            return new double[] { xyz[0], xyz[1], xyz[2] };
        }
    }

    public boolean addToIndex(NeuronVertex vertex) {
        try {
            double[] key = keyForVertex(vertex);
            //log.info("Adding to new index: ({},{},{})",key[0],key[1],key[2]);
            index.insert(key, vertex);
            // Store original key, in case the old position changes
            cachedKeys.put(vertex, cacheableKey(key));
        }
        catch (KeySizeException | KeyDuplicateException ex) {
            return false;
        }
        return true;
    }

    public boolean removeFromIndex(NeuronVertex vertex) {
        try {
            double[] k = keyForVertex(vertex);
            index.delete(k);
            cachedKeys.remove(vertex);
        }
        catch (KeySizeException | KeyMissingException ex) {
            return false;
        }
        return true;
    }
    
    public boolean updateIndex(NeuronVertex vertex) {
        if (!removeFromIndex(vertex)) {
            return false;
        }
        return addToIndex(vertex);
    }

    private CacheableKey cacheableKey(double[] key) {
        return new CacheableKey(key);
    }
    
    public boolean isValid() {
        return valid.get();
    }
    
    public synchronized void rebuildIndex(Collection<NeuronModel> neuronList) {
        log.info("Rebuilding spatial index");
        valid.set(false);
        clear();
        for (NeuronModel neuronModel : neuronList) {
            for (NeuronVertex neuronVertex : neuronModel.getVertexes()) {
                addToIndex(neuronVertex);
            }
        }
        valid.set(true);
        log.info("Added {} vertices to spatial index", index.size());
    }
    
    public void clear() {
        this.index = new KDTree<>(3);
    }
    
    private class CacheableKey {

        private final double[] data;
        
        public CacheableKey(double[] data) {
            this.data = Arrays.copyOf(data, 3);
        }
        
        public double[] toArray() {
            return data;
        }
        
    }
}
