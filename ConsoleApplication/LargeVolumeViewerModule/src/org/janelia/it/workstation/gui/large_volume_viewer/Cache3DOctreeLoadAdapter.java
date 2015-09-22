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
    public LoadTimer loadTimer = new LoadTimer();
    
    public Cache3DOctreeLoadAdapter() {
        super();
        tileFormat.setIndexStyle(TileIndex.IndexStyle.OCTREE);
        // Report performance statistics when program closes
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                loadTimer.report();
            }
        });
        
    }
    
    @Override
    public TextureData2dGL loadToRam(TileIndex tileIndex) throws TileLoadError, MissingTileException {
        return loadToRam(tileIndex, true);
    }
    
    public TextureData2dGL loadToRam(TileIndex tileIndex, boolean zOriginNegativeShift) throws TileLoadError, MissingTileException {
        LoadTimer localLoadTimer = new LoadTimer();
        localLoadTimer.mark("starting slice load");
        // Create a local load timer to measure timings just in this thread
        final File octreeFilePath = OctreeMetadataSniffer.getOctreeFilePath(tileIndex, tileFormat, zOriginNegativeShift);
        if (octreeFilePath == null) {
            log.warn("Returning null: octree file path null.");
            return null;
        }

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

        TextureData2dGL result = loadSlice(relativeSlice, folder, tileIndex.getSliceAxis());
        localLoadTimer.mark("finished slice load");

        loadTimer.putAll(localLoadTimer);
        return result;
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
    
    private synchronized TextureData2dGL loadSlice(int relativeZ, File folder, CoordinateAxis axis) {
        try {
            log.info("Loading tile {}, for Z-plane {}.", Utilities.trimToOctreePath(folder), relativeZ);
            TextureData2dGL tex = new TextureData2dGL();
            final int sc = tileFormat.getChannelCount();

            String tiffBase = OctreeMetadataSniffer.getTiffBase(axis);
            StringBuilder missingTiffs = new StringBuilder();
            StringBuilder requestedTiffs = new StringBuilder();
            CacheFacadeI cacheManager = CacheController.getInstance().getManager();

            byte[][] allBuffers = new byte[sc][];

            int totalBufferSize = sc * sliceSize;
            for (int c = 0; c < sc; ++c) {
                allBuffers[c] = null;
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
                } else {
                    byte[] tiffBytes = getBytes(tiff, cacheManager);
                    if (tiffBytes != null) {
                        allBuffers[c] = tiffBytes;
                    } else {
                        log.error("Tiff bytes are null.");
                    }
                }
            }

            // Interleave into a buffer.
            int bytesPerVoxel = tileFormat.getBitDepth() / 8;

            // Multiple-of-8 width.
            final int tileW = tileFormat.getTileSize()[0];
            final int widthExtraVoxels = 8 - (tileW % 8);
            final int widthExtraBytes = widthExtraVoxels * sc * bytesPerVoxel;
            final int tileH = tileFormat.getTileSize()[1];
            final int paddedBufferSize = totalBufferSize + tileH * widthExtraBytes;

            ByteBuffer pixels = ByteBuffer.allocate(paddedBufferSize);
            byte[] dummyPixels = new byte[bytesPerVoxel];
            byte[] stuffer = new byte[widthExtraBytes];
            pixels.rewind();
            int sliceOffset = sliceSize * relativeZ;
            int i = 0;
            for (int h = 0; h < tileH; h++) {
                for (int w = 0; w < tileW; w++) {
                    for (int c = 0; c < sc; c++) {
                        if (allBuffers[c] == null) {
                            pixels.put(dummyPixels);
                        }
                        else {
                            final int byteRunStart = sliceOffset + i * bytesPerVoxel;
                            //for ( int vb = 0; vb < bytesPerVoxel; vb++) {
                            //    if ( allBuffers[c][byteRunStart + vb] != 0 ) {
                            //        nonZeroCount[c] ++;
                            //    }
                            //}
                            pixels.put(allBuffers[c], byteRunStart, bytesPerVoxel);
                        }
                    }
                    i++;
                }
                pixels.put(stuffer);
            }
            pixels.rewind();

            // Push into the final image.
            tex.setChannelCount(sc);
            tex.setUsedWidth(tileW);
            tex.setWidth(tileW + widthExtraVoxels);
            tex.setHeight(tileH);
            log.info("Setting width={}, usedWidth={}, height={}.", tex.getWidth(), tex.getUsedWidth(), tex.getHeight());
            tex.setSwapBytes(false);
            tex.setPixels(pixels);
            tex.setBitDepth(tileFormat.getBitDepth());
            tex.updateTexImageParams();

            //tex.loadRenderedImage(composite);
            return tex;
        } catch (Exception ex) {
            log.error("Error in loading slice: {}", ex.getMessage());
            ex.printStackTrace();
            return null;
        }
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
            log.info("Cache has {} at the ready.", Utilities.trimToOctreePath(tiff));
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
