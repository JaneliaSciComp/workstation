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
import java.util.concurrent.ThreadFactory;
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
    private int standardFileSize = 0;
    
    public CachePopulator() {
        this.executor = Executors.newFixedThreadPool(THREAD_COUNT, new CustomNamedThreadFactory());
    }
    
    public void setStandadFileSize(int size) {
        standardFileSize = size;
    }
    
    public Map<String,CacheFacade.CachableWrapper> retargetCache(GeometricNeighborhood neighborhood, boolean cancelOld, Cache cache) {
        Map<String,CacheFacade.CachableWrapper> populatedMap = new HashMap<>();
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
                    if (standardFileSize > 0) {
                        byte[] bytes = new byte[standardFileSize];
                        Future<byte[]> future = cache(file, bytes);
                        compressedDataFutures.put(key, future);
                        CacheFacade.CachableWrapper wrapper = new CacheFacade.CachableWrapper(future, bytes);
                        populatedMap.put(key, wrapper);
                    }
                    else {
                        throw new IllegalArgumentException("Pre-sized byte array required.");
                    }
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

    /**
     * Push one file to cache.
     *
     * @param file which file.
     * @return 'future' version.
     */
    public Future<byte[]> cache(File file, byte[] storage) {
        return executor.submit(new CachePopulatorWorker(file, storage));
    }
    
    private static class CustomNamedThreadFactory implements ThreadFactory {

        private static int _threadNum = 1;
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("CachePopulatorThread_" + _threadNum++);
            return t;
        }
        
    }
}
