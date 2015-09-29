package org.janelia.it.workstation.cache.large_volume;

/**
 * Implement this to hear about new geo neighborhoods being created.
 *
 * @author fosterl
 */
public interface GeometricNeighborhoodListener {
    void created( GeometricNeighborhood neighborhood );    
}
