/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

import com.sun.media.jai.codec.SeekableStream;
import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
//import java.net.URL;
//import org.apache.jcs.JCS;
//import org.apache.jcs.access.CacheAccess;
//import org.apache.jcs.access.exception.CacheException;
//import org.apache.jcs.engine.control.CompositeCacheManager;

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
public class CacheFacade {
    private static final String CACHE_NAME = "CompressedTiffCache";
    private static final boolean CACHE_OFLOW_DISK = false;
    private static final boolean CACHE_ETERNAL = false;
    private static final int CACHE_MAX_IN_MEM = 10000;
    // Time-to-* settings are about making things stale, so that new versions
    // could be fetched.  However, these applications hit precomputed (not
    // dynamically updated) repositories.  Nothing will change during the
    // session.
    private static final int CACHE_TIME2LIVE_S = 0; // Do not evict based on time spent in cache.
    private static final int CACHE_TIME2IDLE_S = 0; // Do not evict based on time between accesses.
    
    private GeometricNeighborhoodBuilder neighborhoodBuilder;
    private GeometricNeighborhood neighborhood;
    private CachePopulator cachePopulator;
    //private CacheAccess jcs;
    private CacheManager manager;
    private CompressedFileResolver resolver = new CompressedFileResolver();
    private Double cameraZoom;
    
    private Logger log = LoggerFactory.getLogger(CacheFacade.class);
    
    /**
     * This is the guardian around the cache implementation.  Note that
     * neither Future nor SeekableStream are serializable, so any notion of
     * using a non-memory cache is impossible without some redesign.
     * 
     * @param region unique key for cache namespace.
     * @throws Exception from called methods.
     */
    public CacheFacade(String region) throws Exception {
//        CompositeCacheManager ccm = CompositeCacheManager.getUnconfiguredInstance();
//        Properties props = new Properties();
//
//        props.put("jcs.default", "DC");
//        props.put("jcs.default.cacheattributes",
//                "org.apache.jcs.engine.CompositeCacheAttributes");
//
//        ccm.configure(props);
//        Properties props = new Properties();
//        props.load(this.getClass().getResourceAsStream("/cache.ccf"));
//        CompositeCacheManager ccm = CompositeCacheManager.getUnconfiguredInstance();
//        ccm.configure(props);

//        URL url = getClass().getResource("/ehcacheCompressedTiff.xml");
//        manager = CacheManager.create(url);
        // Establishing in-memory cache, programmatically.
        manager = CacheManager.create();
        Cache memoryOnlyCache = new Cache(
                CACHE_NAME, 
                CACHE_MAX_IN_MEM, 
                CACHE_OFLOW_DISK, 
                CACHE_ETERNAL, 
                CACHE_TIME2LIVE_S, 
                CACHE_TIME2IDLE_S
        );
        manager.addCache(memoryOnlyCache);
        
        //jcs = CacheAccess.getAccess(REGION_NAME); // Seed with defaults.
        //jcs = JCS.getInstance(region);
        cachePopulator = new CachePopulator();
    }

    public CacheFacade() throws Exception {
        this(CACHE_NAME);
    }

    /**
     * Getters with a future. Makes the caller await completion of the
     * task that is supplying the compressed byte array.
     * 
     * @param id what to dig up.
     * @return result: a compressed stream of bytes.
     */
	public Future<byte[]> getFuture(String id) {
        Future<byte[]> rtnVal = null;
        Cache cache = manager.getCache(CACHE_NAME);
        Element cachedElement = cache.get(id);
        if (cachedElement != null) {
            CachableWrapper wrapper = (CachableWrapper) cachedElement.getValue();
            if (wrapper != null) {
                rtnVal = wrapper.getWrappedObject();
            }
        }
		return rtnVal;
	}

	public Future<byte[]> getFuture(File file) {
        return getFuture(file.getAbsolutePath());
	}

    /**
     * Blocking getters. Let the cache manager worry about the threading.
     * Most likely ID is an absolute path.  Also, packages a decompressed
     * version of the cached data, into a seekable stream.
     * 
     * @see CacheManager#get(java.io.File) calls this.
     * @param id what to dig up.
     * @return result, after awaiting the future, or null on exception.
     */
    public SeekableStream get(final String id) {
        log.info("Getting {}", id);
        SeekableStream rtnVal = null;
        try {
            Future<byte[]> futureBytes = getFuture(id);
            if (futureBytes == null) {
                log.warn("No Future found for {}. Pushing data into cache.", id);
                GeometricNeighborhood gn = new GeometricNeighborhood() {
                    @Override
                    public Set<File> getFiles() {
                        Set<File> rtnVal = new HashSet<>();
                        rtnVal.add(new File(id));
                        return rtnVal;
                    }
                    
                };
                populateRegion(gn, false); // Add this, but do not kill old neighborhood.
                futureBytes = getFuture(id);
            }

            if (futureBytes != null   &&   !futureBytes.isCancelled()) {
                byte[] compressedData = futureBytes.get();
                rtnVal = resolver.resolve(compressedData, new File(id));
            }

        } catch (InterruptedException | ExecutionException ie) {
            log.warn("Interrupted thread, while returning {}.", id);
        } catch (Exception ex) {
            log.error("Failure to resolve cached version of {}", id);
            ex.printStackTrace();
        }
        return rtnVal;
    }

    /**
     * This getter takes the name of the decompressed version of the input file.
     * 
     * @param file decompressed file's name.
     * @return stream of data, fully decompressed.
     */
    public SeekableStream get(File file) {
        // The key stored in the cache is the compressed file name.
        File compressedFile = resolver.compressAs(file);        
        return get(compressedFile.getAbsolutePath());
    }
    

    /**
     * When focus is set/changed, the neighborhood builder goes to work,
     * and the cache is populated.
     * 
     * @todo get the zoom level as number or object, into this calculation.
     * @param focus neighborhood is around this point.
     */
	public void setFocus(double[] focus) {
        log.info("Setting focus...");
        if (calculateRegion(focus)) {
            populateRegion();
        }
	}
    
    public void setCameraZoom(Double zoom) {
        this.cameraZoom = zoom;
    }

    /**
     * @return the neighborhoodBuilder
     */
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
    public void setNeighborhoodBuilder(GeometricNeighborhoodBuilder neighborhoodBuilder) {
        this.neighborhoodBuilder = neighborhoodBuilder;
        
    }

    protected boolean calculateRegion(double[] focus) {
        boolean rtnVal = false;
        GeometricNeighborhood calculatedNeighborhood = neighborhoodBuilder.buildNeighborhood(focus, cameraZoom);
        if (!(calculatedNeighborhood.getFiles().isEmpty()  ||  calculatedNeighborhood.equals(this.neighborhood))) {
            this.neighborhood = calculatedNeighborhood;
            rtnVal = true;
        }
        return rtnVal;
    }

    protected void populateRegion() {
        populateRegion(neighborhood, true);
    }
    
    protected void populateRegion(GeometricNeighborhood neighborhood, boolean cancelOld) {
        Map<String, Future<byte[]>> futureArrays = cachePopulator.populateCache(neighborhood, cancelOld);
        Cache cache = manager.getCache(CACHE_NAME);
        for (String id : futureArrays.keySet()) {
            Future<byte[]> futureArray = futureArrays.get(id);
            CachableWrapper wrapper = new CachableWrapper(futureArray);
            cache.put(new Element(id, wrapper));
//            try {
//                jcs.putSafe(id, futureArray);
//            } catch (CacheException ce) {
//                log.warn("Failed to put {}'s value into cache.  Error '{}'.", id, ce.getMessage());
//                ce.printStackTrace();
//            }
        }
    }

    /**
     * May require serializable objects.  Neither 'Future' nor its generic 
     * target will ever be serializable.  However, since our cache resides
     * at all times in memory, making the cached object transient should not 
     * be problematic.
     */
    private static class CachableWrapper implements Serializable {
        private transient Future<byte[]> wrappedObject;
        public CachableWrapper(Future<byte[]> object) {
            wrappedObject = object;
        }
        
        public Future<byte[]> getWrappedObject() {
            return wrappedObject;
        }
    }

}
