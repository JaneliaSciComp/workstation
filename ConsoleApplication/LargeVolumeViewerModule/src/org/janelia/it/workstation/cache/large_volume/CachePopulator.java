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
import java.util.concurrent.Callable;
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
    private final CacheCollection cacheCollection;
    private int standardFileSize = 0;
    private boolean extractFromContainerFormat = false;
    
    private Logger log = LoggerFactory.getLogger(CachePopulator.class);
        
    public CachePopulator(CacheCollection cacheCollection) {
        this.fileLoadExecutor = Executors.newFixedThreadPool(FILE_READ_THREAD_COUNT, new CustomNamedThreadFactory(FILE_READ_THREAD_PREFIX));
        this.memAllocExecutor = Executors.newFixedThreadPool(MEM_ALLOC_THREAD_COUNT, new CustomNamedThreadFactory(MEM_ALLOC_THREAD_PREFIX));
        this.cacheCollection = cacheCollection;
    }
    
    public void setStandadFileSize(int size) {
        standardFileSize = size;
    }
    
    public int getStandardFileSize() {
        return standardFileSize;
    }
    
    public Collection<String> retargetCache(GeometricNeighborhood neighborhood) {
        final Set<String> populatedList = new HashSet<>();
        // Now, repopulate the running queue with the new neighborhood.
        for (final File file : neighborhood.getFiles()) {
            Callable<Void> callable = new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    try {
                        allocateAndLaunch(file, populatedList);
                    } catch (Exception ex) {
                        log.error("Failed to allocate-and-launch {}, reason {}.", file, ex.getMessage());
                    }                            
                    return null;
                }
            };
            memAllocExecutor.submit(callable);
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
     * @return the extractFromContainerFormat
     */
    public boolean isExtractFromContainerFormat() {
        return extractFromContainerFormat;
    }

    /**
     * @param extractFromContainerFormat the extractFromContainerFormat to set
     */
    public void setExtractFromContainerFormat(boolean extractFromContainerFormat) {
        this.extractFromContainerFormat = extractFromContainerFormat;
    }
    
    public CachableWrapper pushLaunch(String id, byte[] storage) {
        CachableWrapper wrapper = wrapDataAndFetch(new File(id), storage);
        cacheCollection.put(id, wrapper);
        
        return wrapper;
    }

    public void close() {
        fileLoadExecutor.shutdown();
        memAllocExecutor.shutdown();
    }

    private void allocateAndLaunch(File file, Set<String> populatedList) {
        final String key = file.getAbsolutePath();
        if (! cacheCollection.hasKey(key) && (!populatedList.contains(key))) {
            if (standardFileSize > 0) {
                byte[] bytes = cacheCollection.getStorage(key);
                CachableWrapper wrapper = wrapDataAndFetch(file, bytes);                
                cacheCollection.put(file.getAbsolutePath(), wrapper);
                populatedList.add(key);
            } else {
                throw new IllegalArgumentException("Pre-sized byte array required.");
            }
        } else {
            log.debug("In cache {}.", Utilities.trimToOctreePath(key));
        }
    }

    /**
     * Pull one file's data into the given storage.
     *
     * @param file which file.
     * @return 'future' version.
     */
    private Future<byte[]> launchDataFetch(File file, byte[] storage) {
        log.info("Making a cache populator worker for {}.", Utilities.trimToOctreePath(file.getAbsolutePath()));
        Callable cachePopulatorWorker = null;
        if (isExtractFromContainerFormat()) {
            cachePopulatorWorker = new ExtractedCachePopulatorWorker(file, storage);
        } else {
            cachePopulatorWorker = new CachePopulatorWorker(file, storage);
        }
        return fileLoadExecutor.submit(cachePopulatorWorker);
    }

    private CachableWrapper wrapDataAndFetch(File file, byte[] bytes) {
        Future<byte[]> future = launchDataFetch(file, bytes);
        CachableWrapper wrapper = new CachableWrapper(future, bytes);
        return wrapper;
    }

}
