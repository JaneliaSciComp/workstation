package org.janelia.it.workstation.gui.large_volume_viewer;

import java.io.File;
import java.nio.ByteBuffer;
import org.janelia.it.workstation.cache.large_volume.CacheController;
import org.janelia.it.workstation.cache.large_volume.CacheFacadeI;
import org.janelia.it.workstation.cache.large_volume.ExtractedCachePopulatorWorker;
import org.janelia.it.workstation.cache.large_volume.Utilities;
import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.gui.large_volume_viewer.exception.DataSourceInitializeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This load adapter will employ 3D, not 2D caching.
 * 
 * @author fosterl
 */
public class Cache3DOctreeLoadAdapter extends AbstractTextureLoadAdapter {

    private static final Logger log = LoggerFactory.getLogger(Cache3DOctreeLoadAdapter.class);

    // Metadata: file location required for local system as mount point.
    private File topFolder;
    private String remoteBasePath;
    private int standardVolumeSize;
    private boolean acceptNullDecoders = true;
    private int sliceSize = -1;
    
    @Override
    public TextureData2dGL loadToRam(TileIndex tileIndex) throws TileLoadError, MissingTileException {
        return loadToRam(tileIndex, true);
    }
    
    public TextureData2dGL loadToRam(TileIndex tileIndex, boolean zOriginNegativeShift) throws TileLoadError, MissingTileException {
        // Create a local load timer to measure timings just in this thread
        LoadTimer localLoadTimer = new LoadTimer();
        localLoadTimer.mark("starting slice load");
        final File octreeFilePath = OctreeMetadataSniffer.getOctreeFilePath(tileIndex, tileFormat, zOriginNegativeShift);
        if (octreeFilePath == null) {
            return null;
        }
		// TODO - generalize to URL, if possible
        // (though TIFF requires seek, right?)
        // Compute octree path from Raveler-style tile indices
        File folder = new File(topFolder, octreeFilePath.toString());

		// TODO for debugging, show file name for X tiles
        // Compute local z slice
        int zoomScale = (int) Math.pow(2, tileIndex.getZoom());
        int axisIx = tileIndex.getSliceAxis().index();
        int tileDepth = tileFormat.getTileSize()[axisIx];
        int absoluteSlice = (tileIndex.getCoordinate(axisIx)) / zoomScale;
        int relativeSlice = absoluteSlice % tileDepth;
        // Raveller y is flipped so flip when slicing in Y (right?)
        if (axisIx == 1) {
            relativeSlice = tileDepth - relativeSlice - 1;
        }

        return loadSlice(relativeSlice, tileIndex, folder, tileIndex.getSliceAxis());
    }

    public File getTopFolder() {
        return topFolder;
    }
    
    /** Order dependency: call this before setTopFolder. */
    public void setRemoteBasePath( String remoteBasePath ) {
        this.remoteBasePath = remoteBasePath;
    }    

    public void setTopFolder(File topFolder) throws DataSourceInitializeException {
        this.topFolder = topFolder;
        final OctreeMetadataSniffer octreeMetadataSniffer = new OctreeMetadataSniffer(topFolder, tileFormat);
        octreeMetadataSniffer.setRemoteBasePath(remoteBasePath);
        octreeMetadataSniffer.sniffMetadata(topFolder);
        standardVolumeSize = octreeMetadataSniffer.getStandardVolumeSize();
        sliceSize = octreeMetadataSniffer.getSliceSize();
		// Don't launch pre-fetch yet.
        // That must occur AFTER volume initialized signal is sent.
    }
    
    public int getStandardFileSize() {
        return standardVolumeSize;
    }
    
    private TextureData2dGL loadSlice(int relativeZ, TileIndex tileIndex, File folder, CoordinateAxis axis) {
        
        TextureData2dGL tex = new TextureData2dGL();
        final int sc = tileFormat.getChannelCount();
        
        String tiffBase = OctreeMetadataSniffer.getTiffBase(axis);
        StringBuilder missingTiffs = new StringBuilder();
        StringBuilder requestedTiffs = new StringBuilder();
        CacheFacadeI cacheManager = CacheController.getInstance().getManager();
        if (cacheManager == null) {
            return null;
        }

        byte[][] allBuffers = new byte[sc][];
        
        int totalBufferSize = sc * sliceSize;
        for (int c = 0; c < sc; ++c) {
            allBuffers[c] = new byte[sliceSize];
            // Need to establish the channels, out of data extracted from cache.
            File tiff = new File(folder, OctreeMetadataSniffer.getFilenameForChannel(tiffBase, c));
            if (requestedTiffs.length() > 0) {
                requestedTiffs.append("; ");
            }
            requestedTiffs.append(tiff);
            if (!tiff.exists()) {
                if (acceptNullDecoders) {
                    if (missingTiffs.length() > 0) {
                        missingTiffs.append(", ");
                    }
                    missingTiffs.append(tiff);
                }
            }
            else {
                byte[] tiffBytes = getBytes(tiff, cacheManager);
                if ( tiffBytes != null ) {
                    try {
                        // Save to buffer collection
                        allBuffers[c] = tiffBytes;
                    } catch ( RuntimeException rte ) {
                        log.error("System exception during read of bytes");
                        rte.printStackTrace();
                    }
                }
                else {
                    log.error("Tiff bytes are null.");
                }
            }
        }
        
        // Interleave into a buffer.
        ByteBuffer pixels = ByteBuffer.allocate(totalBufferSize);
        pixels.rewind();
        int bytesPerVoxel = tileFormat.getBitDepth() / 8;
        int sliceOffset = sliceSize * relativeZ;
        for ( int i = 0; i < sliceSize; i += bytesPerVoxel ) {
            for (int c = 0; c < sc; ++c) {
                final int byteRunStart = sliceOffset + i * bytesPerVoxel;
                // *** TEMP *** Checking for non-zeros.
                //for ( int vb = 0; vb < bytesPerVoxel; vb++) {
                //    if ( allBuffers[c][byteRunStart + vb] != 0 ) {
                //        nonZeroCount[c] ++;
                //    }
                //}
                pixels.put( allBuffers[c], byteRunStart, bytesPerVoxel );
            }
        }
        
        pixels.rewind();
        
        // Push into the final image.
        tex.setPixels(pixels);
        //tex.loadRenderedImage(composite);
        return tex;
    }
    
    /**
     * Obtain bytes.  Will either pull the tiff's data through the cache
     * mechanism, if it is already prepared, or directly from the disk,
     * if not.  Doing direct-fetch will avoid problems with cache blocking,
     * during new-focus changes.  Although redundant, this will allow the
     * process to move forward.  This mechanism is wasteful compared to the
     * older slice-from-TIFF mechanism, and might need improvement. LLF
     * 
     * @param tiff file to fetch
     * @param cacheManager check this first
     * @return bytes obtained.
     */
    private byte[] getBytes(File tiff, CacheFacadeI cacheManager) {
        byte[] tiffBytes;
        if (cacheManager.isReady(tiff)) {
            tiffBytes = cacheManager.getBytes(tiff);           
        }
        else {
            log.info("Bypassing cache for {}.", Utilities.trimToOctreePath(tiff));
            tiffBytes = new byte[standardVolumeSize];
            try {
                ExtractedCachePopulatorWorker populatorWorker = new ExtractedCachePopulatorWorker(tiff, tiffBytes);
                populatorWorker.readBytes();
            } catch (Exception ex) {
                log.error("Failure during cache bypass. {}", ex.getMessage());
                ex.printStackTrace();
            }
        }
        
        return tiffBytes;
    }
    
}
