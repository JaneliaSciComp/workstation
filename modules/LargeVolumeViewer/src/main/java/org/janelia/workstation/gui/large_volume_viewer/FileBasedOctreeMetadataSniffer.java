package org.janelia.workstation.gui.large_volume_viewer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.media.jai.RenderedImageAdapter;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.SeekableStream;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;
import org.janelia.workstation.geom.CoordinateAxis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this on octree-tile repositories, to get data about the files
 * contained therein.
 *
 * @author fosterl
 */
public class FileBasedOctreeMetadataSniffer {
    private final static Logger log = LoggerFactory.getLogger(FileBasedOctreeMetadataSniffer.class);
    private static final String CHANNEL_0_STD_TIFF_NAME = "default.0.tif";

    /**
     * Return path to tiff file containing a particular slice
     */
    public static Path getOctreeFilePath(TileIndex tileIndex, TileFormat tileFormat) {
        List<String> pathComps = getOctreePath(tileIndex, tileFormat);
        if (pathComps == null) {
            return null;
        } else {
            return Paths.get("", pathComps.toArray(new String[0]));
        }
    }

    /**
     * Return path components to tiff file containing a particular slice
     */
    private static List<String> getOctreePath(TileIndex tileIndex, TileFormat tileFormat) {
        int axIx = tileIndex.getSliceAxis().index();

        List<String> pathComps = new ArrayList<>();
        int octreeDepth = tileFormat.getZoomLevelCount();
        int depth = octreeDepth - tileIndex.getZoom();
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
            int scale = 1 << (depth - 2 - d);
            int ds[] = {
                    xyz[0] / scale,
                    xyz[1] / scale,
                    xyz[2] / scale
            };

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
                log.info("Bad tile index {} because of {} at zoom level {}", tileIndex, ds, d);
                return null;
            }
            // offset x/y/z for next deepest level
            for (int i = 0; i < 3; ++i) {
                xyz[i] = xyz[i] % scale;
            }

            // Octree coordinates are in z-order
            int octreeCoord = 1 + ds[0]
                    + 2 * (1 - ds[1]) // Raveler Y is at bottom; octree Y is at top
                    + 4 * ds[2];

            pathComps.add(String.valueOf(octreeCoord));
        }
        return pathComps;
    }

    static String getFilenameForChannel(String tiffBase, int c) {
        return tiffBase + "." + c + ".tif";
    }

    static String getTiffBase(CoordinateAxis axis) {
        String tiffBase = "default"; // Z-view; XY plane
        if (axis == CoordinateAxis.Y) {
            tiffBase = "ZX";
        } else if (axis == CoordinateAxis.X) {
            tiffBase = "YZ";
        }
        return tiffBase;
    }

    private final File topFolder;
    private final TileFormat tileFormat;
    private final CoordinateToRawTransform transform;

    FileBasedOctreeMetadataSniffer(File topFolder, TileFormat tileFormat) {
        this.topFolder = topFolder;
        this.tileFormat = tileFormat;
        this.transform = new CoordinateToRawTransform(topFolder);
    }

    void retrieveMetadata() {
        try {
            // Set some default parameters, to be replaced my measured parameters
            tileFormat.setDefaultParameters();

            // Count color channels by counting channel files
            tileFormat.setChannelCount(0);
            int channelCount = 0;
            while (true) {
                File tiff = new File(topFolder, "default." + channelCount + ".tif");
                if (!tiff.exists()) {
                    break;
                }
                channelCount += 1;
            }
            tileFormat.setChannelCount(channelCount);
            if (channelCount < 1) {
                return;
            }

            // X and Y slices?
            tileFormat.setHasXSlices(new File(topFolder, "YZ.0.tif").exists());
            tileFormat.setHasYSlices(new File(topFolder, "ZX.0.tif").exists());
            tileFormat.setHasZSlices(new File(topFolder, CHANNEL_0_STD_TIFF_NAME).exists());

            // Deduce octree depth from directory structure depth
            int octreeDepth = 0;
            File deepFolder = topFolder;
            File deepFile = new File(topFolder, CHANNEL_0_STD_TIFF_NAME);
            while (deepFile.exists()) {
                octreeDepth += 1;
                File parentFolder = deepFolder;
                // Check all possible children: some might be empty
                for (int branch = 1; branch <= 8; ++branch) {
                    deepFolder = new File(parentFolder, "" + branch);
                    deepFile = new File(deepFolder, CHANNEL_0_STD_TIFF_NAME);
                    if (deepFile.exists()) {
                        break; // found a deeper branch
                    }
                }
            }
            int zoomFactor = (int) Math.pow(2, octreeDepth - 1);
            tileFormat.setZoomLevelCount(octreeDepth);

            // Deduce other parameters from first image file contents
            File tiff = new File(topFolder, CHANNEL_0_STD_TIFF_NAME);
            SeekableStream s = new FileSeekableStream(tiff);
            ImageDecoder decoder = ImageCodec.createImageDecoder("tiff", s, null);
            // Z dimension is related to number of tiff pages
            int sz = decoder.getNumPages();

            // Get X/Y dimensions from first image
            RenderedImageAdapter ria = new RenderedImageAdapter(
                    decoder.decodeAsRenderedImage(0));
            int sx = ria.getWidth();
            int sy = ria.getHeight();

            log.info("SX={}, SY={}, SZ={}.", sx, sy, sz);

            // Full volume could be much larger than this downsampled tile
            int[] tileSize = new int[3];
            tileSize[2] = sz;
            if (sz < 1) {
                return;
            }
            tileSize[0] = sx;
            tileSize[1] = sy;
            tileFormat.setTileSize(tileSize);

            int[] volumeSize = new int[3];
            volumeSize[2] = zoomFactor * sz;
            volumeSize[0] = zoomFactor * sx;
            volumeSize[1] = zoomFactor * sy;
            tileFormat.setVolumeSize(volumeSize);

            int bitDepth = ria.getColorModel().getPixelSize();
            tileFormat.setBitDepth(bitDepth);
            tileFormat.setIntensityMax((int) Math.pow(2, bitDepth) - 1);

            tileFormat.setSrgb(ria.getColorModel().getColorSpace().isCS_sRGB());

            // Setup the origin and the scale.
            updateOriginAndScale();

        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void updateOriginAndScale() {
        int[] origin = transform.getOrigin();
        double[] scale = transform.getScale();
        // Scale must be converted to micrometers.
        for (int i = 0; i < scale.length; i++) {
            scale[i] /= 1000; // nanometers to micrometers
        }
        // Origin must be divided by 1000, to convert to micrometers.
        for (int i = 0; i < origin.length; i++) {
            origin[i] = (int) (origin[i] / (1000 * scale[i])); // nanometers to voxels
        }
        tileFormat.setVoxelMicrometers(scale);
        tileFormat.setOrigin(origin);
    }

}

