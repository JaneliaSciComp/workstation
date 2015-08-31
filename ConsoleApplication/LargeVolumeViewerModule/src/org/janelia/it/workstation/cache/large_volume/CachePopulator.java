/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.sf.ehcache.Cache;

/**
 * Build up the cache, in response to some new neighborhood of locations
 * being handed in.
 * 
 * @author fosterl
 */
public class CachePopulator {
    private static final int THREAD_COUNT = 10;
    private final ExecutorService executor;
    private GeometricNeighborhood neighborhood;
    private final Map<String,Future<byte[]>> compressedDataFutures = new HashMap<>();
    
    public CachePopulator() {
        this.executor = Executors.newFixedThreadPool(THREAD_COUNT);
    }
    
    public Map<String,Future<byte[]>> retargetCache(GeometricNeighborhood neighborhood, boolean cancelOld, Cache cache) {
        Map<String,Future<byte[]>> populatedMap = new HashMap<>();
        // Avoid re-launching on non-significant movement.
        if (! neighborhood.equals(this.neighborhood)) {
            this.neighborhood = neighborhood;
            // Kill any non-running tasks in the executor.
            if (cancelOld) {
                for (String key : compressedDataFutures.keySet()) {
                    Future<byte[]> futureShock = compressedDataFutures.get(key);
                    // false: cancel any which do not require interruption.
                    boolean wasCancelled = futureShock.cancel(false);
                    if (wasCancelled) {
                        cache.remove(key);
                    }
                }                
                compressedDataFutures.clear();
            }
            // Now, repopulate the running queue with the new neighborhood.
            // Any co-inciding un-processed items will simply be re-prioritized
            // according to the new focus' relative position.
            for (File file: neighborhood.getFiles()) {
                final String key = file.getAbsolutePath();
                if (! compressedDataFutures.containsKey(key)  &&  cache.get(key) == null) {                    
                    Future<byte[]> future = cache(file);
                    compressedDataFutures.put(key, future);
                    populatedMap.put(key, future);
                }
            }
        }
        return populatedMap;
        
    }
    
    /**
     * Push one file to cache.
     * 
     * @param file which file.
     * @return 'future' version.
     */
    public Future<byte[]> cache(File file) {
        return executor.submit(new CachePopulatorWorker(file));
    }
}
