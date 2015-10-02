/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.SeekableStream;
import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.janelia.it.workstation.gui.large_volume_viewer.compression.CompressedFileResolver;

/**
 * This class will take a neighborhood builder, and will use that to populate
 * cache, centered around the given focus.  It will return what is needed
 * by the client, as long is it is in the cache.
 * 
 * @author fosterl
 */
public class EHCountedCacheFacade extends AbstractCacheFacade implements CacheFacadeI {
    // TODO use more dynamic means of determining how many of the slabs
    // to put into memory at one time.
    
    private static final int CACHE_FUDGE = 12;
    
    private GeometricNeighborhoodBuilder neighborhoodBuilder;
    private GeometricNeighborhood neighborhood;
    private CachePopulator cachePopulator;
    //private CacheAccess jcs;
    private CacheManager manager;
    private final CompressedFileResolver resolver = new CompressedFileResolver();
    private CompressedFileResolver.CompressedFileNamer compressNamer;
    private Double cameraZoom;
    private double[] cameraFocus;
    private double pixelsPerSceneUnit;
    private String cacheName;
    private int totalGets;
    private int noFutureGets;
    private int standardFileSize;
    
    private int cacheCount = 0;
    private Map<String,AllocationUnit> inUseStorage = new HashMap<>();
    
    private static Logger log = LoggerFactory.getLogger(EHCountedCacheFacade.class);
    
    /**
     * This is the guardian around the cache implementation.  Note that
     * neither Future nor SeekableStream are serializable, so any notion of
     * using a non-memory cache is impossible without some redesign.
     * 
     * @param region unique key for cache namespace.
     * @throws Exception from called methods.
     */
    public EHCountedCacheFacade(String region, final int standardFileSize) throws Exception {
        // Establishing in-memory cache, declaratively.
        this.standardFileSize = standardFileSize;
        log.debug("Creating a cache {}.", region);
        cacheName = region;
        URL url = getClass().getResource("/ehcacheCountedTiff.xml");
        manager = CacheManager.create(url);                
        
        // Get close to a maximum, so that we are not very tight on cache
        // size, before releasing.
        cacheCount = (int)((CACHE_MEMORY_SIZE_GB * (long)GIGA)/standardFileSize) - 10;
        allocateStorage();
        
        CacheCollection toolkit = new CacheCollection() {            
            @Override
            public void put(String id, CachableWrapper wrapper) {
                final Element element = new Element(id, wrapper);
                Cache cache = manager.getCache(cacheName);
                cache.put(element);
            }
            @Override
            public boolean hasKey(String id) {
                Cache cache = manager.getCache(cacheName);
                return isInCache(id);
            }
            @Override
            public byte[] getStorage(String id) {
                return reuseStorage(id);
            }
        };
        Cache cache = manager.getCache(cacheName);
        CacheConfiguration config = cache.getCacheConfiguration();
        if (! config.isFrozen()) {
            // Add fudge margin, so that there is something to shove into the
            // cache, to force other entries to go away.
            long maxEntries = cacheCount - CACHE_FUDGE;
            log.debug("Setting max entries value to {} for heap storage {}.", maxEntries, config.getMaxBytesLocalHeap());
            config.setMaxEntriesLocalHeap(maxEntries);
        }
        else {
            log.debug("Configuration frozen: cannot adjust load factor.");
        }
        
        cachePopulator = new CachePopulator(toolkit);
        cachePopulator.setStandadFileSize(standardFileSize);
        cachePopulator.setExtractFromContainerFormat(true);
    }

    public EHCountedCacheFacade(int standardFileSize) throws Exception {
        this(CACHE_NAME, standardFileSize);
    }
    
    @Override
    public void close() {
        cachePopulator.close();
    }

    /**
     * This getter takes the name of the decompressed version of the input file.
     * 
     * @param file decompressed file's name.
     * @return stream of data, fully decompressed.
     */
    @Override
    public SeekableStream get(File file) {
        // The key stored in the cache is the compressed file name.
        File compressedFile = compressNamer.getCompressedName(file);        
        return get(compressedFile.getAbsolutePath());
    }    
    
    @Override
    public byte[] getBytes(File file) {
        return getBytes(file.getAbsolutePath());
    }

    /**
     * Tell if this file is not only in cache, but will have no Future.get
     * wait time.
     *
     * @param file decompressed file's name.
     * @return true -> no waiting for cache.
     */
    @Override
    public boolean isReady(File file) {
        boolean rtnVal = false;
        // The key stored in the cache is the compressed file name.
        if (compressNamer == null) {
            compressNamer = resolver.getNamer(file);
        }
        File compressedFile = compressNamer.getCompressedName(file);
        Cache cache = manager.getCache(cacheName);
        final String id = compressedFile.getAbsolutePath();
        if (isInCache(id)) {
            CachableWrapper wrapper = (CachableWrapper) cache.get(id).getObjectValue();            
            Future wrappedObject = wrapper.getWrappedObject();
            rtnVal = wrappedObject == null  ||  wrappedObject.isDone();
        }
        if (! rtnVal) {
            noFutureGets ++;
            totalGets ++;
        }
        return rtnVal;
    }

    /**
     * When focus is set/changed, the neighborhood builder goes to work,
     * and the cache is populated.
     * 
     * @todo get the zoom level as number or object, into this calculation.
     * @param focus neighborhood is around this point.
     */
    @Override
	public synchronized void setFocus(double[] focus) {
        log.trace("Setting focus from thread {}.", Thread.currentThread().getName());
        try {
            cameraFocus = focus;
            updateRegion();
        } catch (Exception ex) {
            log.error("Exception at set-camera-focus.");
            ex.printStackTrace();
        }
	}

    /**
     * Set the zoom, but do not trigger any changes.  Allow trigger to be
     * pulled downstream.
     */
    @Override
    public void setCameraZoomValue(Double zoom) {
        log.debug("Setting zoom {}....", zoom);
        this.cameraZoom = zoom;
    }
    
    @Override
    public void setPixelsPerSceneUnit(double pixelsPerSceneUnit) {
        if (pixelsPerSceneUnit < 1.0) {
            pixelsPerSceneUnit = 1.0;
        }
        this.pixelsPerSceneUnit = pixelsPerSceneUnit;
    }
    
    /**
     * Change camera zoom, and trigger any cascading updates.
     */
    @Override
    public void setCameraZoom(Double zoom) {
        try {
            setCameraZoomValue(zoom);
            // Force a recalculation, based on both focus and zoom.
            if (this.cameraFocus != null) {
                log.debug("Re-Setting focus {}.", cameraFocus);
                updateRegion();
            }
        } catch ( Exception ex ) {
            log.error("Exception at set-camera-zoom.");
            ex.printStackTrace();
        }
    }

    /**
     * @return the neighborhoodBuilder
     */
    @Override
    public GeometricNeighborhoodBuilder getNeighborhoodBuilder() {
        return neighborhoodBuilder;
    }

    /**
     * Supply this from without, to allow for external configuration by caller.
     *     Ex: neighborhoodBuilder =
     *             new WorldExtentSphereBuilder(tileFormat, 10000);
     * 
     * @param neighborhoodBuilder the neighborhoodBuilder to set
     */
    @Override
    public void setNeighborhoodBuilder(GeometricNeighborhoodBuilder neighborhoodBuilder) {
        this.neighborhoodBuilder = neighborhoodBuilder;
        
    }

    /**
     * For testing purposes.
     */
    @Override
    public void dumpKeys() {
        Cache cache = manager.getCache(cacheName);
        List keys = cache.getKeys();
        System.out.println("All Keys:=-----------------------------------: " + keys.size());
        for (Object key : keys) {
            System.out.println("KEY:" + key);
        }
    }

    private boolean isInCache(String id) {
        Cache cache = manager.getCache(cacheName);
        return isInCache(cache, id);
    }

    private boolean isInCache(Cache cache, String id) throws CacheException, IllegalStateException {
        return cache.isKeyInCache(id);
    }

    private void updateRegion() {
        if (calculateRegion()) {
            log.debug("Populating a new region.  At least some files differed.");
            populateRegion();
        }
    }

    /** Make all the storage that will ever be used in our cache. */
    private synchronized void allocateStorage() {
        try {
            for (int i = 0; i < this.cacheCount; i++) {
                AllocationUnit au = new AllocationUnit();
                au.setStorageBinNumber(i);
                inUseStorage.put("Unused Storage " + i, au);
            }
        } catch (Exception ex) {
            log.error("Failed to allocate required storage {}.", ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    private synchronized byte[] reuseStorage(String targetId) {
        log.trace("Starting reuse Storage from thread {}.", Thread.currentThread().getName());
        Cache cache = manager.getCache(cacheName);
        byte[] rtnVal = null;
        AllocationUnit oldUnit = null;
        String oldKey = null;
        for (String key: inUseStorage.keySet()) {
            if (! isInCache(cache, key)) {
                // Has been evicted since having been allocated.
                oldUnit = inUseStorage.get( key );
                oldKey = key;
                break;
            }
        }
        
        if (oldUnit != null) {
            inUseStorage.remove( oldKey );
            // Found one we can reuse.
            AllocationUnit au = new AllocationUnit();
            if (oldUnit.getStorage() == null) {
                // Allocate as needed.
                final byte[] storage = new byte[standardFileSize];
                au.setStorage(storage);
            }
            else {
                au.setStorage(oldUnit.getStorage());
            }
            au.setStorageBinNumber(oldUnit.getStorageBinNumber());            
            rtnVal = au.getStorage();
            inUseStorage.put( targetId, au );
        }
        else {
            // Got a problem.
            log.error("No reusable storage available.");
            throw new IllegalStateException("Insufficient storage available.");
        }
        log.trace("Return from reuse Storage");
        
        return rtnVal;
    }
    
    /**
     * Blocking getters. Let the cache manager worry about the threading. Most
     * likely ID is an absolute path. Also, packages a decompressed version of
     * the cached data, into a seekable stream.
     *
     * @see CacheManager#get(java.io.File) calls this.
     * @param id what to dig up.
     * @return result, after awaiting the future, or null on exception.
     */
    private SeekableStream get(final String id) {
        SeekableStream rtnVal = null;
        try {
            final byte[] bytes = getBytes(id);            
            if (bytes != null) {
                rtnVal = new ByteArraySeekableStream(bytes);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return rtnVal;
    }

    private byte[] getBytes(final String id) {        
        String keyOnly = Utilities.trimToOctreePath(id);
        log.debug("Getting {}", keyOnly);
        totalGets++;
        byte[] rtnVal = null;
        try {
            CachableWrapper wrapper = null;
            Cache cache = manager.getCache(cacheName);
            if (isInCache(id)) {                
                wrapper = (CachableWrapper) cache.get(id).getObjectValue();
                log.debug("Returning {}: found in cache.", keyOnly);
            } else {
                byte[] storage = reuseStorage( id );
                wrapper = cachePopulator.pushLaunch(id, storage);
                log.debug("Missing.  Returning {}: pushed to cache. Zoom={}.", keyOnly, cameraZoom);
            }
            log.trace("Getting {} from cache wrapper.", keyOnly);
            dumpCacheMemoryUse(cache);
            rtnVal = wrapper.getBytes();
            //Utilities.zeroScan(rtnVal, "CacheFacade.getBytes()", keyOnly);
            log.trace("Returning from cache wrapper: {}.", keyOnly);
        } catch (InterruptedException | ExecutionException ie) {
            log.warn("Interrupted thread, while returning {}.  Message: {}", id, ie.getMessage());
            ie.printStackTrace();
        } catch (Exception ex) {
            log.error("Failure to resolve cached version of {}", id);
            ex.printStackTrace();
        }
        if (rtnVal == null) {
            log.warn("Ultimately returning a null value for {}.", id);
        }

        if (totalGets % 50 == 0) {
            log.trace("No-future get ratio: {}/{} = {}.", noFutureGets, totalGets, ((double) noFutureGets / (double) totalGets));
        }
        return rtnVal;
    }
    
    private void dumpCacheMemoryUse(Cache cache) {
        List<String> keys = cache.getKeys();
        final double required = (double)((long)keys.size() * (long)cachePopulator.getStandardFileSize()) / (double)GIGA;
        log.debug("======> Required memory is {}", required);
    }

    private boolean calculateRegion() {
        boolean rtnVal = false;
        GeometricNeighborhood calculatedNeighborhood = neighborhoodBuilder.buildNeighborhood(cameraFocus, cameraZoom, pixelsPerSceneUnit);
        if (isUpdatedNeighborhood(calculatedNeighborhood)) {
            this.neighborhood = calculatedNeighborhood;
            rtnVal = true;
        }
        return rtnVal;
    }

    private boolean isUpdatedNeighborhood(GeometricNeighborhood calculatedNeighborhood) {
        if (calculatedNeighborhood == null) {
            log.warn("Null neighborhood after calculation.");
        }
        return calculatedNeighborhood != null  &&  (!(calculatedNeighborhood.getFiles().isEmpty()  ||  calculatedNeighborhood.equals(neighborhood)));
    }

    private void populateRegion() {
        populateRegion(neighborhood);
    }
    
    private void populateRegion(GeometricNeighborhood neighborhood) {
        log.debug("Retargeting cache at zoom {}.", cameraZoom);
        Collection<String> futureArrays = cachePopulator.retargetCache(neighborhood);
        if (log.isDebugEnabled()) {
            for (String id : futureArrays) {
                log.trace("Populating {} to cache at zoom {}.", Utilities.trimToOctreePath(id), cameraZoom);
            }
        }
    }

    public static class NonNeighborhoodCachableWrapper extends CachableWrapper {
        public NonNeighborhoodCachableWrapper(Future<byte[]> object, byte[] bytes) {
            super(object, bytes);
        }
    }

    /** Bean to keep track of who owns what. */
    private static class AllocationUnit {
        private byte[] storage;
        private int storageBinNumber;

        /**
         * @return the storage
         */
        public byte[] getStorage() {
            return storage;
        }

        /**
         * @param storage the storage to set
         */
        public void setStorage(byte[] storage) {
            this.storage = storage;
        }

        /**
         * @return the storageBinNumber
         */
        public int getStorageBinNumber() {
            return storageBinNumber;
        }

        /**
         * @param storageBinNumber the storageBinNumber to set
         */
        public void setStorageBinNumber(int storageBinNumber) {
            this.storageBinNumber = storageBinNumber;
        }
    }
}
