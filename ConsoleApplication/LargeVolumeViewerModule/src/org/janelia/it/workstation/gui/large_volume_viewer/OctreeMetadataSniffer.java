package org.janelia.it.workstation.gui.large_volume_viewer;

/**
 * Created by murphys on 11/6/2015.
 */
import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.SeekableStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.media.jai.RenderedImageAdapter;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.gui.large_volume_viewer.exception.DataSourceInitializeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this on octree-tile repositories, to get data about the files
 * contained therein.
 *
 * @author fosterl
 */
public class OctreeMetadataSniffer {
    private final static Logger log = LoggerFactory.getLogger(OctreeMetadataSniffer.class);
    public static final String CHANNEL_0_STD_TIFF_NAME = "default.0.tif";

    private final File topFolder;
    private String remoteBasePath;
    private final TileFormat tileFormat;

    public OctreeMetadataSniffer(File topFolder, TileFormat tileFormat) {
        this.topFolder = topFolder;
        this.tileFormat = tileFormat;
    }

    public File getTopFolder() {
        return topFolder;
    }

    public void setRemoteBasePath( String remoteBasePath ) {
        this.remoteBasePath = remoteBasePath;
    }

    public boolean sniffOriginAndScaleFromFolder(int[] origin, double[] scale) {
        File transformFile = new File(getTopFolder(), "transform.txt");
        if (!transformFile.exists()) {
            return false;
        }
        try {
            Pattern pattern = Pattern.compile("([os][xyz]): (\\d+(\\.\\d+)?)");
            Scanner scanner = new Scanner(transformFile);
            double scaleScale = 1.0 / Math.pow(2, tileFormat.getZoomLevelCount() - 1);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                Matcher m = pattern.matcher(line);
                if (m.matches()) {
                    String key = m.group(1);
                    String value = m.group(2);
                    switch (key) {
                        case "ox":
                            origin[0] = Integer.parseInt(value);
                            break;
                        case "oy":
                            origin[1] = Integer.parseInt(value);
                            break;
                        case "oz":
                            origin[2] = Integer.parseInt(value);
                            break;
                        case "sx":
                            scale[0] = Double.parseDouble(value) * scaleScale;
                            break;
                        case "sy":
                            scale[1] = Double.parseDouble(value) * scaleScale;
                            break;
                        case "sz":
                            scale[2] = Double.parseDouble(value) * scaleScale;
                            break;
                        default:
                            break;
                    }
                }
            }
            // TODO
            return true;
        } catch (FileNotFoundException ex) {
        }

        return false;
    }

    // Workaround for loading origin and scale without web services
    public void sniffOriginAndScale() throws DataSourceInitializeException {
        int[] origin = new int[3];
        double[] scale = new double[3];
        try {
            CoordinateToRawTransform transform
                    = ModelMgr.getModelMgr().getCoordToRawTransform(remoteBasePath);
            origin = transform.getOrigin();
            scale = transform.getScale();
        } catch (Exception ex) {
            if (!sniffOriginAndScaleFromFolder(origin, scale)) {
                throw new DataSourceInitializeException(
                        "Failed to find metadata", ex
                );
            }
        }
        // Scale must be converted to micrometers.
        for (int i = 0; i < scale.length; i++) {
            scale[ i] /= 1000; // nanometers to micrometers
        }
        // Origin must be divided by 1000, to convert to micrometers.
        for (int i = 0; i < origin.length; i++) {
            origin[ i] = (int) (origin[i] / (1000 * scale[i])); // nanometers to voxels
        }

        tileFormat.setVoxelMicrometers(scale);
        // Shifting everything by ten voxels to the right.
        int[] mockOrigin = new int[]{
                0,//origin[0],
                origin[1],
                origin[2]
        };
        // tileFormat.setOrigin(mockOrigin);
        tileFormat.setOrigin(origin);
    }

    public void sniffMetadata(File topFolderParam)
            throws DataSourceInitializeException {
        try {

            // Set some default parameters, to be replaced my measured parameters
            tileFormat.setDefaultParameters();

            // Count color channels by counting channel files
            tileFormat.setChannelCount(0);
            int channelCount = 0;
            while (true) {
                File tiff = new File(topFolderParam, "default." + channelCount + ".tif");
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
            tileFormat.setHasXSlices(new File(topFolderParam, "YZ.0.tif").exists());
            tileFormat.setHasYSlices(new File(topFolderParam, "ZX.0.tif").exists());
            tileFormat.setHasZSlices(new File(topFolderParam, CHANNEL_0_STD_TIFF_NAME).exists());

            // Deduce octree depth from directory structure depth
            int octreeDepth = 0;
            File deepFolder = topFolderParam;
            File deepFile = new File(topFolderParam, CHANNEL_0_STD_TIFF_NAME);
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
            File tiff = new File(topFolderParam, CHANNEL_0_STD_TIFF_NAME);
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
            if (remoteBasePath != null) {
                sniffOriginAndScale();
            }
            // TODO - actual max intensity

        } catch (Exception ex) {
            throw new DataSourceInitializeException(
                    "Failed to find metadata", ex
            );
        }
    }

    /**
     * Volume size is enough to hold the extracted volume of one of the
     * files.  It will not hold multiple channels.  It is "post digestion"
     * of some format like TIF.
     *
     * @return size in bytes.
     */
    public int getStandardVolumeSize() {
        return getSliceSize() * tileFormat.getTileSize()[2];
    }

    public int getSliceSize() {
        return tileFormat.getTileSize()[0] * tileFormat.getTileSize()[1] * tileFormat.getBitDepth() / 8;
    }

    public static int computeDepth(int octreeDepth, TileIndex tileIndex) {
        return octreeDepth - tileIndex.getZoom();
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
                log.debug("Bad tile index " + tileIndex);
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
        File file = new File(topFolderParam, CHANNEL_0_STD_TIFF_NAME);
        if (file.exists()) {
            return (int) file.length();
        } else {
            throw new IllegalArgumentException("Invalid root directory.  No " + CHANNEL_0_STD_TIFF_NAME);
        }
    }

    public static String getFilenameForChannel(String tiffBase, int c) {
        return tiffBase + "." + c + ".tif";
    }

    public static String getTiffBase(CoordinateAxis axis) {
        String tiffBase = "default"; // Z-view; XY plane
        if (axis == CoordinateAxis.Y) {
            tiffBase = "ZX";
        } else if (axis == CoordinateAxis.X) {
            tiffBase = "YZ";
        }
        return tiffBase;
    }

}

