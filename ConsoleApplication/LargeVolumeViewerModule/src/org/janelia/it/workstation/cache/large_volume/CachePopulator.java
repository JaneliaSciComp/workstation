/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

import org.janelia.it.workstation.gui.large_volume_viewer.CustomNamedThreadFactory;
import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Build up the cache, in response to some new neighborhood of locations
 * being handed in.
 * 
 * @author fosterl
 */
public class CachePopulator {
    private static final int FILE_READ_THREAD_COUNT = 12;
    private static final int MEM_ALLOC_THREAD_COUNT = 3;
    private static final String FILE_READ_THREAD_PREFIX = "CacheFileReaderThread";
    private static final String MEM_ALLOC_THREAD_PREFIX = "CacheByteAllocThread";
    private final ExecutorService fileLoadExecutor;
    private final ExecutorService memAllocExecutor;
//    private GeometricNeighborhood neighborhood;
    private int standardFileSize = 0;
    private Logger log = LoggerFactory.getLogger(CachePopulator.class);
    
    public CachePopulator() {
        this.fileLoadExecutor = Executors.newFixedThreadPool(FILE_READ_THREAD_COUNT, new CustomNamedThreadFactory(FILE_READ_THREAD_PREFIX));
        this.memAllocExecutor = Executors.newFixedThreadPool(MEM_ALLOC_THREAD_COUNT, new CustomNamedThreadFactory(MEM_ALLOC_THREAD_PREFIX));
    }
    
    public void setStandadFileSize(int size) {
        standardFileSize = size;
    }
    
    public Collection<String> retargetCache(GeometricNeighborhood neighborhood, final Cache cache) {
        final Set<String> populatedList = new HashSet<>();
        // Now, repopulate the running queue with the new neighborhood.
        for (final File file : neighborhood.getFiles()) {
            Runnable r = new Runnable() {
                public void run() {
                    allocateAndLaunch(file, cache, populatedList);
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
    
    public void close() {
        fileLoadExecutor.shutdown();
        memAllocExecutor.shutdown();
    }

    private void allocateAndLaunch(File file, Cache cache, Set<String> populatedList) {
        final String key = file.getAbsolutePath();
        if (cache.get(key) == null && (!populatedList.contains(key))) {
            if (standardFileSize > 0) {
                byte[] bytes = new byte[standardFileSize];
                Future<byte[]> future = cache(file, bytes);
                CacheFacade.CachableWrapper wrapper = new CacheFacade.CachableWrapper(future, bytes);
                
                populateElement(file.getAbsolutePath(), wrapper, cache);
                populatedList.add(key);
            } else {
                throw new IllegalArgumentException("Pre-sized byte array required.");
            }
        } else {
            log.debug("In cache as {}.  Not populating {}.", cache.get(key).getObjectValue().getClass().getSimpleName(), trimToOctreePath(key));
        }
    }

    private void populateElement(String id, CacheFacade.CachableWrapper wrapper, Cache cache) throws IllegalStateException, CacheException, IllegalArgumentException {
        final Element element = new Element(id, wrapper);
        cache.put(element);
    }        
    
    /**
     * Push one file to cache.
     * 
     * @param file which file.
     * @return 'future' version.
     */
    public Future<byte[]> cache(File file) {
        return fileLoadExecutor.submit(new CachePopulatorWorker(file));
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
    
    /**
     * Push one file to cache.
     *
     * @param file which file.
     * @return 'future' version.
     */
    public Future<byte[]> cache(File file, byte[] storage) {
        return fileLoadExecutor.submit(new CachePopulatorWorker(file, storage));
    }
    
}
