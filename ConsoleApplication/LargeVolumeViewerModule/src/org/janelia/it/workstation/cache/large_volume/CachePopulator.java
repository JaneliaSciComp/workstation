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
    
    public Map<String,Future<byte[]>> populateCache(GeometricNeighborhood neighborhood) {
        Map<String,Future<byte[]>> populatedMap = new HashMap<>();
        // Avoid re-launching on non-significant movement.
        if (! neighborhood.equals(this.neighborhood)) {
            this.neighborhood = neighborhood;
            // Kill any non-running tasks in the executor.
            for (String key: compressedDataFutures.keySet()) {
                Future<byte[]> futureShock = compressedDataFutures.get(key);
                // false: cancel any which do not require interruption.
                futureShock.cancel(false);                
            }
            compressedDataFutures.clear();
            // Now, repopulate the running queue with the new neighborhood.
            // Any co-inciding un-processed items will simply be re-prioritized
            // according to the new focus' relative position.
            for (File file: neighborhood.getFiles()) {
                Future<byte[]> future = executor.submit(new CachePopulatorWorker( file ));
                final String key = file.getAbsolutePath();
                compressedDataFutures.put( key, future );
                populatedMap.put( key, future );
//                Future<SeekableStream> seekableFuture = executor.submit(new CachePopulatorWorker(file));
//                final String key = file.getAbsolutePath();
//                streamFutures.put(key, seekableFuture);
//                populatedMap.put(key, seekableFuture);
            }
        }
        return populatedMap;
        
    }
}
