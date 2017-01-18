package org.janelia.it.workstation.gui.large_volume_viewer.neuron_api;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.util.MatrixUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    private Jama.Matrix micronToVoxMatrix;
    private KDTree<NeuronVertex> index; // Cannot be final because it doesn't have a clear method
    
    public NeuronVertexSpatialIndex() {
        log.trace("Creating spatial index");
    }
            
    public void initSample(TmSample sample) {
        log.info("Initializing spatial index with sample={}", sample.getId());
        // TODO: this matrix should get deserialized once when the sample is loaded, not all over the place
        this.micronToVoxMatrix = MatrixUtilities.deserializeMatrix(sample.getMicronToVoxMatrix(), "micronToVoxMatrix");
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
     * Returns the anchor that is closest to the location given in voxel units.
     * @param voxelXYZ
     * @return
     */
    public NeuronVertex getAnchorClosestToVoxelLocation(double[] voxelXYZ) {
        if (index==null) return null;
        List<NeuronVertex> nbrs = getAnchorClosestToVoxelLocation(voxelXYZ, 1);
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
        if (micronToVoxMatrix==null) {
            log.warn("No sample loaded in spatial index");
            return Collections.emptyList();
        }

        // Convert from Cartesian micrometers to image voxel coordinates
        // NeuronVertex API requires coordinates in micrometers
        Jama.Matrix micLoc = new Jama.Matrix(new double[][] {
            {micronXYZ[0], }, 
            {micronXYZ[1], }, 
            {micronXYZ[2], },
            {1.0, },
        });
        // TmGeoAnnotation XYZ is in voxel coordinates
        Jama.Matrix voxLoc = micronToVoxMatrix.times(micLoc);
        double[] voxelXYZ = new double [] { voxLoc.get(0,0), voxLoc.get(1,0), voxLoc.get(2,0) };
        return getAnchorClosestToVoxelLocation(voxelXYZ, n);
    }
    
    /**
     * Returns the N closest anchors to the location given in voxel units. The locations are sorted in 
     * order from closest to farthest.
     * @param voxelXYZ
     * @param n
     * @return
     */
    public List<NeuronVertex> getAnchorClosestToVoxelLocation(double[] voxelXYZ, int n) {
        if (index==null) return Collections.emptyList();
        try {
            return index.nearest(voxelXYZ, n);
        } 
        catch (KeySizeException ex) {
            log.warn("Exception while finding anchor in spatial index", ex);
            return null;
        }
    }

    /**
     * Returns all the anchors found in the area given by two corners points, given in voxel units.  
     * @param p1 lower corner
     * @param p2 higher corner
     * @return list of anchors 
     */
    public List<NeuronVertex> getAnchorsInVoxelArea(double[] p1, double[] p2) {
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
            float xyz[] = v.getLocation(); // Neuron API returns coordinates in voxels
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

    public void rebuildIndex(Collection<NeuronModel> neuronList) {
        
        // TODO: This is called twice whenever a neuron is merged because both mergeNeurite 
        // and deleteNeuron incorrectly call fireWorkspaceLoaded. That should be fixed in those methods.  
        log.info("Rebuilding spatial index");
        clear();
        for (NeuronModel neuronModel : neuronList) {
            for (NeuronVertex neuronVertex : neuronModel.getVertexes()) {
                addToIndex(neuronVertex);
            }
        }
        log.info("Added {} vertices to spatial index with {} cached keys", index.size(), cachedKeys.size());
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
