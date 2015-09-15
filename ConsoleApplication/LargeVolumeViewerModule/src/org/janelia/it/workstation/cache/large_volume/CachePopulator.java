/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

import org.janelia.it.workstation.gui.large_volume_viewer.CustomNamedThreadFactory;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Build up the cache, in response to some new neighborhood of locations
 * being handed in.
 * 
 * @author fosterl
 */
public class CachePopulator {
    private static final int FILE_READ_THREAD_COUNT = 1;
    private static final int MEM_ALLOC_THREAD_COUNT = 1;
    private static final String FILE_READ_THREAD_PREFIX = "CacheFileReaderThread";
    private static final String MEM_ALLOC_THREAD_PREFIX = "CacheByteAllocThread";
    private final ExecutorService fileLoadExecutor;
    private final ExecutorService memAllocExecutor;
    private final CacheAcceptor acceptor;
//    private GeometricNeighborhood neighborhood;
    private int standardFileSize = 0;
    private Logger log = LoggerFactory.getLogger(CachePopulator.class);
        
    public CachePopulator(CacheAcceptor acceptor) {
        this.fileLoadExecutor = Executors.newFixedThreadPool(FILE_READ_THREAD_COUNT, new CustomNamedThreadFactory(FILE_READ_THREAD_PREFIX));
        this.memAllocExecutor = Executors.newFixedThreadPool(MEM_ALLOC_THREAD_COUNT, new CustomNamedThreadFactory(MEM_ALLOC_THREAD_PREFIX));
        this.acceptor = acceptor;
    }
    
    public void setStandadFileSize(int size) {
        standardFileSize = size;
    }
    
    public Collection<String> retargetCache(GeometricNeighborhood neighborhood) {
        final Set<String> populatedList = new HashSet<>();
        // Now, repopulate the running queue with the new neighborhood.
        for (final File file : neighborhood.getFiles()) {
            Runnable r = new Runnable() {
                public void run() {
                    allocateAndLaunch(file, populatedList);
                }
            };
            memAllocExecutor.submit(r);
        }
        if (neighborhood.getFiles().isEmpty()) {
            double[] focus = neighborhood.getFocus();
            log.warn(
                "Neighborhood of size 0, at zoom {}.  Focus {},{},{}.", neighborhood.getZoom(), focus[0], focus[1], focus[2]
            );
        }
        return populatedList;

    }
    
    /**
     * Push one file to cache.
     *
     * @param file which file.
     * @return 'future' version.
     */
    public Future<byte[]> cache(File file, byte[] storage) {
        log.info("Making a cache populator worker for {}.", trimToOctreePath(file.getAbsolutePath()));
        return fileLoadExecutor.submit(new CachePopulatorWorker(file, storage));
    }

    public void close() {
        fileLoadExecutor.shutdown();
        memAllocExecutor.shutdown();
    }

    private void allocateAndLaunch(File file, Set<String> populatedList) {
        final String key = file.getAbsolutePath();
        if (! acceptor.hasKey(key) && (!populatedList.contains(key))) {
            if (standardFileSize > 0) {
                byte[] bytes = new byte[standardFileSize];
                Future<byte[]> future = cache(file, bytes);
                CachableWrapper wrapper = new CachableWrapper(future, bytes);
                
                populateElement(file.getAbsolutePath(), wrapper);
                populatedList.add(key);
            } else {
                throw new IllegalArgumentException("Pre-sized byte array required.");
            }
        } else {
            log.debug("In cache {}.", trimToOctreePath(key));
        }
    }
    
    private void populateElement(String id, CachableWrapper wrapper) {
        acceptor.put(id, wrapper);        
    }
    
    String trimToOctreePath(String id) {
        if (id.endsWith(".tif")) {
            int endPoint = id.lastIndexOf("/");
            int startPoint = 0;
            boolean foundAlpha = false;
            while (! foundAlpha) {
                endPoint --;
                if (Character.isAlphabetic(id.charAt(endPoint))) {
                    foundAlpha = true;
                    startPoint = endPoint;
                    startPoint = id.indexOf("/", startPoint);
                }
            }
            return id.substring(startPoint);
        }
        return id;
    }
    
    public static interface CacheAcceptor {
        void put(String id, CachableWrapper wrapper);
        boolean hasKey(String id);
    }
    
}
