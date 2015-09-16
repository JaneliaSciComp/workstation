package org.janelia.it.workstation.cache.large_volume;

import com.sun.media.jai.codec.SeekableStream;
import java.io.File;

/**
 * Implement this to create a 3D cache.
 * @author fosterl
 */
public interface CacheFacadeI {

    void close();

    /**
     * For testing purposes.
     */
    void dumpKeys();

    //public void reportCacheOccupancy() {
    //    Cache cache = manager.getCache(cacheName);
    //    List keys = cache.getKeys();
    //    int requiredGb = (int)(((long)standardFileSize * (long)keys.size()) / (GIGA));
    //    log.info("--- Cache has {} keys.  {}gb required.", keys.size(), requiredGb);
    //}
    /**
     * This getter takes the name of the decompressed version of the input file.
     *
     * @param file decompressed file's name.
     * @return stream of data, fully decompressed.
     */
    SeekableStream get(File file);

    /**
     * @return the neighborhoodBuilder
     */
    GeometricNeighborhoodBuilder getNeighborhoodBuilder();

    /**
     * Tell if this file is not only in cache, but will have no Future.get
     * wait time.
     *
     * @param file decompressed file's name.
     * @return true -> no waiting for cache.
     */
    boolean isReady(File file);

    /**
     * Change camera zoom, and trigger any cascading updates.
     */
    void setCameraZoom(Double zoom);

    /**
     * Set the zoom, but do not trigger any changes.  Allow trigger to be
     * pulled downstream.
     */
    void setCameraZoomValue(Double zoom);

    /**
     * When focus is set/changed, the neighborhood builder goes to work,
     * and the cache is populated.
     *
     * @todo get the zoom level as number or object, into this calculation.
     * @param focus neighborhood is around this point.
     */
    void setFocus(double[] focus);

    /**
     * Supply this from without, to allow for external configuration by caller.
     *     Ex: neighborhoodBuilder =
     *             new WorldExtentSphereBuilder(tileFormat, 10000);
     *
     * @param neighborhoodBuilder the neighborhoodBuilder to set
     */
    void setNeighborhoodBuilder(GeometricNeighborhoodBuilder neighborhoodBuilder);

    void setPixelsPerSceneUnit(double pixelsPerSceneUnit);
    
}
