/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.SeekableStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
public class MapCacheFacade implements CacheFacadeI {
    public static final int GIGA = 1024*1024*1024;
    // TODO use more dynamic means of determining how many of the slabs
    // to put into memory at one time.
    
    private GeometricNeighborhoodBuilder neighborhoodBuilder;
    private GeometricNeighborhood neighborhood;
    private final CachePopulator cachePopulator;
    //private CacheAccess jcs;
    private final CompressedFileResolver resolver = new CompressedFileResolver();
    private CompressedFileResolver.CompressedFileNamer compressNamer;
    private Double cameraZoom;
    private double[] cameraFocus;
    private double pixelsPerSceneUnit;
    private int totalGets;
    private int noFutureGets;
    
    private int standardFileSize = -1;
    
    private Map<String,CachableWrapper> cacheMap = new HashMap<>();
    private List<byte[]> allocatedBuffers = new ArrayList<>();
    
    private Logger log = LoggerFactory.getLogger(MapCacheFacade.class);
    
    public MapCacheFacade(int standardFileSize) {
        this.standardFileSize = standardFileSize;
        CacheCollection collection = new CacheCollection() {
            @Override
            public void put(String id, CachableWrapper wrapper) {
                cacheMap.put(id, wrapper);
            }

            @Override
            public boolean hasKey(String id) {
                return cacheMap.containsKey(id);
            }
            
            @Override
            public byte[] getStorage(String id) {
                return allocatedBuffers.remove(0);
            }
        };
        cachePopulator = new CachePopulator(collection);
        cachePopulator.setStandadFileSize(standardFileSize);
        cachePopulator.setExtractFromContainerFormat(true);
    }
    
    @Override
    public void close() {
        cachePopulator.close();
    }

    public void reportCacheOccupancy() {
        Set<String> keys = cacheMap.keySet();
        int requiredGb = (int)(((long)standardFileSize * (long)keys.size()) / (GIGA));
        log.info("--- Cache has {} keys.  {}gb required.", keys.size(), requiredGb);
    }

    /**
     * This getter takes the name of the decompressed version of the input file.
     * 
     * @param file decompressed file's name.
     * @return stream of data, fully decompressed.
     */
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
        final String id = compressedFile.getAbsolutePath();
        if (isInCache(id)) {
            CachableWrapper wrapper = (CachableWrapper) cacheMap.get(id);            
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
        cameraFocus = focus;
        updateRegion();
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
        setCameraZoomValue(zoom);
        // Force a recalculation, based on both focus and zoom.
        if (this.cameraFocus != null) {
            updateRegion();
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
        System.out.println("All Keys:=-----------------------------------: " + cacheMap.size());
        Set<String> safeKeySet = new HashSet<>(cacheMap.keySet());
        for (String key: safeKeySet) {
            System.out.println("KEY:" + key);
        }
    }

    private boolean isInCache(String id) {
        return cacheMap.containsKey(id);
    }

    private void updateRegion() {
        if (calculateRegion()) {
            populateRegion();
        }
    }
    
    /**
     * ID is an absolute path. Also, packages a decompressed version of
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
            log.warn("Ultimately returning null for {}.", id);
            ex.printStackTrace();
        }
        return rtnVal;
    }

    /**
     * ID is an absolute path. Also, packages a decompressed version of the
     * cached data, into a seekable stream.
     *
     * @see CacheManager#get(java.io.File) calls this.
     * @param id what to dig up.
     * @return result, after awaiting the future, or null on exception.
     */
    private byte[] getBytes(final String id) {
        String keyOnly = cachePopulator.trimToOctreePath(id);
        log.info("Getting {}", keyOnly);
        totalGets++;
        byte[] rtnVal = null;
        try {
            CachableWrapper wrapper = (CachableWrapper) cacheMap.get(id);
            rtnVal = wrapper.getBytes();
            log.info("Returning {}: found in cache.", keyOnly);
        } catch (InterruptedException | ExecutionException ie) {
            log.warn("Interrupted thread, while returning {}.", id);
        } catch (Exception ex) {
            log.error("Failure to resolve cached version of {}", id);
            ex.printStackTrace();
        }
        if (rtnVal == null) {
            log.warn("Ultimately returning a null value for {}.", id);
        }

        if (totalGets % 50 == 0) {
            log.info("No-future get ratio: {}/{} = {}.", noFutureGets, totalGets, ((double) noFutureGets / (double) totalGets));
        }
        return rtnVal;
    }

    private boolean calculateRegion() {
        Date start = new Date();
        boolean rtnVal = false;
        GeometricNeighborhood calculatedNeighborhood = neighborhoodBuilder.buildNeighborhood(cameraFocus, cameraZoom, pixelsPerSceneUnit);
        if (!(calculatedNeighborhood.getFiles().isEmpty()  ||  calculatedNeighborhood.equals(neighborhood))) {            
//            ensureStorage(calculatedNeighborhood);
            this.neighborhood = mergeNewWithOld(calculatedNeighborhood, this.neighborhood);            
            rtnVal = true;
        }
        Date end = new Date();
        long elapsed = (end.getTime() - start.getTime());
        log.info("In calculate region for: {}ms in thread {}.", elapsed, Thread.currentThread().getName());
        return rtnVal;
    }

    private void populateRegion() {
        log.info("Repopulating on focus.  Zoom={}", cameraZoom);
        populateRegion(neighborhood);
    }

    private void populateRegion(GeometricNeighborhood neighborhood) {
        Date start = new Date();
        log.info("Retargeting cache at zoom {}.", cameraZoom);
        Collection<String> futureArrays = cachePopulator.retargetCache(neighborhood);
        if (log.isDebugEnabled()) {
            for (String id : futureArrays) {
                log.debug("Populating {} to cache at zoom {}.", cachePopulator.trimToOctreePath(id), cameraZoom);
            }
        }
        Date end = new Date();
        log.info("In populateRegion for: {}ms.", end.getTime() - start.getTime());
    }

    private GeometricNeighborhood mergeNewWithOld(GeometricNeighborhood newNh, GeometricNeighborhood oldNh) {
        if (oldNh == null) {
            return newNh;
        }
        Set<File> files = new HashSet<>();
        MergedNeighborhood rtnVal = new MergedNeighborhood(files, newNh.getZoom(), newNh.getFocus());

        files.addAll(newNh.getFiles());

        for (File oldFile : oldNh.getFiles()) {
            if (!newNh.getFiles().contains(oldFile)) {
                final String oldKey = oldFile.getAbsolutePath();
                CachableWrapper oldWrapper = cacheMap.remove(oldKey);
                try { 
                    if (oldWrapper != null) {
                        log.info("Removing the old wrapped object, and freeing its buffer.");
                        final Future<byte[]> wrappedObject = oldWrapper.getWrappedObject();
                        // Kill any previous fill operation.
                        if (wrappedObject != null) {
                            wrappedObject.cancel(true);
                        }
                        allocatedBuffers.add(oldWrapper.getBytes());
                    }
                    else {
                        log.info("No wrapped object");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        // The allocated buffers are the set which are now freed up,
        // from not having been included in the new neighborhood.
        int availableBuffers = allocatedBuffers.size();
        log.info("Previous allocation = {}.  Padding up to {}.", availableBuffers, newNh.getFiles().size());
        
        // Now wish to come up with enough buffers, that the entire new
        // neighborhood is complete.
        for (int i = allocatedBuffers.size(); i < newNh.getFiles().size(); i++ ) {
            byte[] newAllocation = new byte[standardFileSize];
            allocatedBuffers.add(newAllocation);
        }
        log.info("Final allocation = {}.", availableBuffers);
        return rtnVal;
    }

    public static class MergedNeighborhood implements GeometricNeighborhood {
        
        private Set<File> files;
        private Double zoom;
        private double[] focus;
        
        public MergedNeighborhood(Set<File> files, Double zoom, double[] focus) {
            this.files = files;
            this.zoom = zoom;
            this.focus = focus;
        }

        @Override
        public Set<File> getFiles() {
            return files;
        }

        @Override
        public Double getZoom() {
            return zoom;
        }

        @Override
        public double[] getFocus() {
            return focus;
        }
        
    }
}
