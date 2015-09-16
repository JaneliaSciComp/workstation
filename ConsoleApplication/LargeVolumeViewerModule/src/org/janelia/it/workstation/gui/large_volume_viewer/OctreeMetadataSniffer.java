package org.janelia.it.workstation.gui.large_volume_viewer;

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
import static org.janelia.it.workstation.gui.large_volume_viewer.BlockTiffOctreeLoadAdapter.CHANNEL_0_STD_TIFF_NAME;
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
    private final Logger log = LoggerFactory.getLogger(OctreeMetadataSniffer.class);
    
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

    public static int computeDepth(int octreeDepth, TileIndex tileIndex) {
        return octreeDepth - tileIndex.getZoom();
    }

}
