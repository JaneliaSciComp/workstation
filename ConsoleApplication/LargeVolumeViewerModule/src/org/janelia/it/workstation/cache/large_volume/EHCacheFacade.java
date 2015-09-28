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

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.swing.JOptionPane;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.Element;
import org.janelia.it.workstation.gui.dialogs.MemoryCheckDialog;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.OctreeMetadataSniffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.janelia.it.workstation.gui.large_volume_viewer.compression.CompressedFileResolver;
import org.janelia.it.workstation.gui.util.WindowLocator;

/**
 * This class will take a neighborhood builder, and will use that to populate
 * cache, centered around the given focus.  It will return what is needed
 * by the client, as long is it is in the cache.
 * 
 * @author fosterl
 */
public class EHCacheFacade implements CacheFacadeI {
    public static final String CACHE_NAME = "CompressedTiffCache";
    public static final int GIGA = 1024*1024*1024;
    public static final int MAX_3D_CACHE_SIZE = 500;
    public static final int MIN_3D_CACHE_SIZE = 200;

    // TODO use more dynamic means of determining how many of the slabs
    // to put into memory at one time.
    
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
    
    private static Logger log = LoggerFactory.getLogger(EHCacheFacade.class);
    
    /**
     * Possibly temporary: using this class to wrap use of BTOLA, and hide
     * the dependency.  If another way can be found to find the standard
     * TIFF file size, this method need not be used.
     * 
     * @param folderUrl base folder containing TIFF files.
     * @return size of a known one, as returned by BTOLA
     */
    public static int getStandardFileLength(URL folderUrl) {
        try {
            File folder = new File(folderUrl.toURI());
            return OctreeMetadataSniffer.getStandardTiffFileSize(folder);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static boolean usingEhCache() {
        return getNeighborhoodSize() > 0;
    }
    
    public static int getNeighborhoodSize() {
        int rtnVal = 0;
        Integer cache3Dsize = (Integer) SessionMgr.getSessionMgr().getModelProperty(EHCacheFacade.CACHE_NAME);
        if (cache3Dsize != null) {
            rtnVal = cache3Dsize;
        }
        return rtnVal;
    }
    
    public static int checkNeigborhoodSize(int settingInt ) {
        if (settingInt > MAX_3D_CACHE_SIZE) {
            String message = String.format("The 3D cache size setting %d is too large.  Running with %d.", settingInt, MAX_3D_CACHE_SIZE);
            log.warn(message);
            JOptionPane.showMessageDialog(WindowLocator.getMainFrame(), message);
            settingInt = MAX_3D_CACHE_SIZE;
        }
        if (settingInt > 0) {
            // Must warn about memory use.
            MemoryCheckDialog memoryChecker = new MemoryCheckDialog();
            if (! memoryChecker.unusedIfInsufficientMemory("3D Cache", 30, WindowLocator.getMainFrame())) {
                settingInt = 0;
            }                    
        }
        return settingInt;
    }
    
    /**
     * This is the guardian around the cache implementation.  Note that
     * neither Future nor SeekableStream are serializable, so any notion of
     * using a non-memory cache is impossible without some redesign.
     * 
     * @param region unique key for cache namespace.
     * @throws Exception from called methods.
     */
    public EHCacheFacade(String region, final int standardFileSize) throws Exception {
        // Establishing in-memory cache, declaratively.
        log.info("Creating a cache {}.", region);
        cacheName = region;
        URL url = getClass().getResource("/ehcacheCompressedTiff.xml");
        manager = CacheManager.create(url);
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
                return cache.get(id) != null;
            }
            @Override
            public byte[] getStorage(String id) {
                return new byte[standardFileSize];
            }
        };
        Cache cache = manager.getCache(cacheName);
        CacheConfiguration config = cache.getCacheConfiguration();
        /*
        if (! config.isFrozen()) {
            long maxEntries = (long)(0.75 * (double)(config.getMaxBytesLocalHeap() / standardFileSize));
            log.info("Setting max entries value to {} for heap storage {}.", maxEntries, config.getMaxBytesLocalHeap());
            config.setMaxEntriesLocalHeap(maxEntries);
        }
        else {
            log.info("Configuration frozen: cannot adjust load factor.");
        }
        */
        
        cachePopulator = new CachePopulator(toolkit);
        cachePopulator.setStandadFileSize(standardFileSize);
        cachePopulator.setExtractFromContainerFormat(true);
    }

    public EHCacheFacade(int standardFileSize) throws Exception {
        this(CACHE_NAME, standardFileSize);
    }
    
    @Override
    public void close() {
        cachePopulator.close();
    }

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
        log.info("Setting focus...");
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
        Element cachedElement = cache.get(id);
        return (cachedElement != null);
    }

    private void updateRegion() {
        if (calculateRegion()) {
            log.info("Populating a new region.  At least some files differed.");
            populateRegion();
        }
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
        log.info("Getting {}", keyOnly);
        totalGets++;
        byte[] rtnVal = null;
        try {
            CachableWrapper wrapper = null;
            Cache cache = manager.getCache(cacheName);
            if (cache.get(id) != null) {                
                wrapper = (CachableWrapper) cache.get(id).getObjectValue();
                log.info("Returning {}: found in cache.", keyOnly);
            } else {
                byte[] storage = new byte[cachePopulator.getStandardFileSize()];                
                wrapper = cachePopulator.pushLaunch(id, storage);
                log.info("Missing.  Returning {}: pushed to cache. Zoom={}.", keyOnly, cameraZoom);
            }
            log.debug("Getting {} from cache wrapper.", keyOnly);
            dumpCacheMemoryUse(cache);
            rtnVal = wrapper.getBytes();
            //Utilities.zeroScan(rtnVal, "CacheFacade.getBytes()", keyOnly);
            log.debug("Returning from cache wrapper: {}.", keyOnly);
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
            log.debug("No-future get ratio: {}/{} = {}.", noFutureGets, totalGets, ((double) noFutureGets / (double) totalGets));
        }
        return rtnVal;
    }
    
    private void dumpCacheMemoryUse(Cache cache) {
        List<String> keys = cache.getKeys();
        final double required = (double)((long)keys.size() * (long)cachePopulator.getStandardFileSize()) / (double)GIGA;
        log.info("======> Required memory is {}", required);
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
        log.info("Retargeting cache at zoom {}.", cameraZoom);
        Collection<String> futureArrays = cachePopulator.retargetCache(neighborhood);
        if (log.isDebugEnabled()) {
            for (String id : futureArrays) {
                log.debug("Populating {} to cache at zoom {}.", Utilities.trimToOctreePath(id), cameraZoom);
            }
        }
    }

    public static class NonNeighborhoodCachableWrapper extends CachableWrapper {
        public NonNeighborhoodCachableWrapper(Future<byte[]> object, byte[] bytes) {
            super(object, bytes);
        }
    }

}
