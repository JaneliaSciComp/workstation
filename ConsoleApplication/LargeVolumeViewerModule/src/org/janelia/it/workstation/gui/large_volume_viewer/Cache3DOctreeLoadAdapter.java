package org.janelia.it.workstation.gui.large_volume_viewer;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import org.janelia.it.workstation.cache.large_volume.CacheController;
import org.janelia.it.workstation.cache.large_volume.CacheFacadeI;
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
    private int standardFileSize;
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

        return null;
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
        standardFileSize = octreeMetadataSniffer.getStandardVolumeSize();
        sliceSize = tileFormat.getTileSize()[0] * tileFormat.getTileSize()[1] * (tileFormat.getBitDepth()/8);
		// Don't launch pre-fetch yet.
        // That must occur AFTER volume initialized signal is sent.
    }
    
    public int getStandardFileSize() {
        return standardFileSize;
    }
    
    private TextureData2dGL loadSlice(int relativeZ, TileIndex tileIndex, File folder, CoordinateAxis axis) {
        
        TextureData2dGL tex = new TextureData2dGL();
        final int sc = tileFormat.getChannelCount();
        RenderedImage[] channels = new RenderedImage[sc];
        String tiffBase = OctreeMetadataSniffer.getTiffBase(axis);
        StringBuilder missingTiffs = new StringBuilder();
        StringBuilder requestedTiffs = new StringBuilder();
        CacheFacadeI cacheManager = CacheController.getInstance().getManager();
        if (cacheManager == null) {
            return null;
        }

        for (int c = 0; c < sc; ++c) {
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
                byte[] tiffBytes = cacheManager.getBytes(tiff);
                if ( tiffBytes != null ) {
                    // Must carve out just the right portion.
                    try {
                        byte[] slice = new byte[sliceSize];
                        System.arraycopy(tiffBytes, sliceSize * relativeZ, slice, 0, sliceSize);
                        channels[sc] = createRenderedImage(slice, tileIndex);
                    } catch ( IOException ioe ) {
                        log.error("IO during read of bytes");
                        ioe.printStackTrace();
                    }
                }
//                if (cacheManager.isReady(tiff)) {
//                    tiffBytes = cacheManager.getBytes(tiff);
//                } else {
//                    // Direct/bypass cache.
//                }
                
            }
        }
        
        // Combine channels into one image
        RenderedImage composite = channels[0];
        if (sc > 1) {
            try {
                ParameterBlockJAI pb = new ParameterBlockJAI("bandmerge");
                for (int c = 0; c < sc; ++c) {
                    pb.addSource(channels[c]);
                }
                composite = JAI.create("bandmerge", pb);
            } catch (NoClassDefFoundError exc) {
                exc.printStackTrace();
                return null;
            }
            // localLoadTimer.mark("merged channels");
        }

        tex.loadRenderedImage(composite);
        return tex;
    }
    
    private RenderedImage createRenderedImage(byte[] tiffBytes, TileIndex tileIndex) throws IOException {
        // Need to check the params.
        //   todo figure out appropriate types.
        BufferedImage rimage = new BufferedImage(tileFormat.getTileSize()[0], tileFormat.getTileSize()[1], BufferedImage.TYPE_INT_RGB); 
        Iterator<?> readers = ImageIO.getImageReadersByFormatName("tiff");
        ImageReader reader = (ImageReader) readers.next();
        ByteArrayInputStream bis = new ByteArrayInputStream(tiffBytes);
        ImageInputStream iis = ImageIO.createImageInputStream(bis);
        ImageReadParam param = reader.getDefaultReadParam();
        Image image = reader.read(0, param);
        Graphics2D g2 = rimage.createGraphics();
        g2.drawImage(image, null, null);
        return rimage;
    }

}
