/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.SeekableStream;
import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

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
    // TODO use more dynamic means of determining how many of the slabs
    // to put into memory at one time.
    
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
        // Establishing in-memory cache, declaratively.
        URL url = getClass().getResource("/ehcacheCompressedTiff.xml");
        manager = CacheManager.create(url);
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
        log.debug("Getting {}", id);
        SeekableStream rtnVal = null;
        try {
            Future<byte[]> futureBytes = getFuture(id);
            if (futureBytes == null) {
                log.warn("No Future found for {}. Pushing data into cache.  Thread: {}.", id, Thread.currentThread().getName());
                // Ensure we get this exact, required file.
                futureBytes = cachePopulator.cache(new File(id));
                Cache cache = manager.getCache(CACHE_NAME);
                log.info("Adding {} to cache.", id);
                cache.put(new Element(id, new CachableWrapper(futureBytes)));
            }

            if (futureBytes != null   &&   !futureBytes.isCancelled()) {                
                byte[] decompressedData = futureBytes.get();
                //rtnVal = resolver.resolve(compressedData, new File(id));
                rtnVal = new ByteArraySeekableStream(decompressedData);
            }

        } catch (InterruptedException | ExecutionException ie) {
            log.warn("Interrupted thread, while returning {}.", id);
        } catch (Exception ex) {
            log.error("Failure to resolve cached version of {}", id);
            ex.printStackTrace();
        }
        if (rtnVal == null) {
            log.warn("Ultimately returning a null value for {}.", id);
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
	public synchronized void setFocus(double[] focus) {
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
        log.info("Repopulating on focus.");
        populateRegion(neighborhood, false); // Cancellation is causing conflicts.
    }
    
    protected void populateRegion(GeometricNeighborhood neighborhood, boolean cancelOld) {
        Cache cache = manager.getCache(CACHE_NAME);
        Map<String, Future<byte[]>> futureArrays = cachePopulator.retargetCache(neighborhood, cancelOld, cache);
        for (String id : futureArrays.keySet()) {
            Future<byte[]> futureArray = futureArrays.get(id);
            CachableWrapper wrapper = new CachableWrapper(futureArray);
            log.debug("Adding {} to cache.", id);
            cache.put(new Element(id, wrapper));
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
