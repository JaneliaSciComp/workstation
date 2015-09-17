package org.janelia.it.workstation.cache.large_volume;

/**
 * Implement this to create a means of dealing with cache for the cache
 * populator.
 * 
 * @author fosterl
 */
public interface CacheCollection {

    void put(String id, CachableWrapper wrapper);
    boolean hasKey(String id);
    byte[] getStorage(String id);
    
}
