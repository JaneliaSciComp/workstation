package org.janelia.it.workstation.gui.large_volume_viewer;

import java.io.File;
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
    
    @Override
    public TextureData2dGL loadToRam(TileIndex tileIndex) throws TileLoadError, MissingTileException {
        return loadToRam(tileIndex, true);
    }
    
    public TextureData2dGL loadToRam(TileIndex tileIndex, boolean zOriginNegativeShift) throws TileLoadError, MissingTileException {
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
        final OctreeMetadataSniffer octreeMetadataSniffer = new OctreeMetadataSniffer(topFolder, new TileFormat());
        octreeMetadataSniffer.setRemoteBasePath(remoteBasePath);
        octreeMetadataSniffer.sniffMetadata(topFolder);
		// Don't launch pre-fetch yet.
        // That must occur AFTER volume initialized signal is sent.
    }

    /*
     * Return path to tiff file containing a particular slice
     */
    public static File getOctreeFilePath(TileIndex tileIndex, TileFormat tileFormat, boolean zOriginNegativeShift) {
        int axIx = tileIndex.getSliceAxis().index();

        File path = new File("");
        int octreeDepth = tileFormat.getZoomLevelCount();
        int depth = OctreeMetadataSniffer.computeDepth(octreeDepth, tileIndex);
        if (depth < 0 || depth > octreeDepth) {
            // This situation can happen in production, owing to missing tiles.
            return null;
        }
        int[] xyz = null;
        xyz = new int[]{tileIndex.getX(), tileIndex.getY(), tileIndex.getZ()};

        // ***NOTE Raveler Z is slice count, not tile count***
        // so divide by tile Z dimension, to make z act like x and y
        xyz[axIx] = xyz[axIx] / tileFormat.getTileSize()[axIx];
        // and divide by zoom scale
        xyz[axIx] = xyz[axIx] / (int) Math.pow(2, tileIndex.getZoom());

        // start at lowest zoom to build up octree coordinates
        for (int d = 0; d < (depth - 1); ++d) {
            // How many Raveler tiles per octant at this zoom?
            int scale = (int) (Math.pow(2, depth - 2 - d) + 0.1);
            int ds[] = {
                xyz[0] / scale,
                xyz[1] / scale,
                xyz[2] / scale};

            // Each dimension makes a binary contribution to the 
            // octree index.
            // Watch for illegal values
            // int ds[] = {dx, dy, dz};
            boolean indexOk = true;
            for (int index : ds) {
                if (index < 0) {
                    indexOk = false;
                }
                if (index > 1) {
                    indexOk = false;
                }
            }
            if (!indexOk) {
                log.error("Bad tile index " + tileIndex);
                return null;
            }
            // offset x/y/z for next deepest level
            for (int i = 0; i < 3; ++i) {
                xyz[i] = xyz[i] % scale;
            }

            // Octree coordinates are in z-order
            int octreeCoord = 1 + ds[0]
                    // TODO - investigate possible ragged edge problems
                    + 2 * (1 - ds[1]) // Raveler Y is at bottom; octree Y is at top
                    + 4 * ds[2];

            path = new File(path, "" + octreeCoord);
        }
        return path;
    }

    public static int getStandardTiffFileSize(File topFolderParam) {
        File file = new File(topFolderParam, OctreeMetadataSniffer.CHANNEL_0_STD_TIFF_NAME);
        if (file.exists()) {
            return (int) file.length();
        } else {
            throw new IllegalArgumentException("Invalid root directory.  No " + OctreeMetadataSniffer.CHANNEL_0_STD_TIFF_NAME);
        }
    }

}
