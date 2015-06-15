package org.janelia.it.workstation.gui.large_volume_viewer;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedImageAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.SeekableStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;

import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.gui.large_volume_viewer.exception.DataSourceInitializeException;
import org.openide.util.Exceptions;

/*
 * Loader for large volume viewer format negotiated with Nathan Clack
 * March 21, 2013.
 * 512x512 tiles
 * Z-order octree folder layout
 * uncompressed tiff stack for each set of slices
 * named like "default.0.tif" for channel zero
 * 16-bit unsigned int
 * intensity range 0-65535
 */
public class BlockTiffOctreeLoadAdapter 
extends AbstractTextureLoadAdapter 
{
	private static final Logger log = LoggerFactory.getLogger(BlockTiffOctreeLoadAdapter.class);

	// Metadata: file location required for local system as mount point.
	private File topFolder;
    // Metadata: file location required for remote system.
    private String remoteBasePath;
	public LoadTimer loadTimer = new LoadTimer();
    
	public BlockTiffOctreeLoadAdapter()
	{
		tileFormat.setIndexStyle(TileIndex.IndexStyle.OCTREE);
		// Report performance statistics when program closes
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				loadTimer.report();
			}
		});
	}
	
    public void setRemoteBasePath(String basePath) {
        remoteBasePath = basePath;
    }
    
	public File getTopFolder() {
		return topFolder;
	}

	public void setTopFolder(File topFolder) 
	throws DataSourceInitializeException
	{
		this.topFolder = topFolder;
		sniffMetadata(topFolder);
		// Don't launch pre-fetch yet.
		// That must occur AFTER volume initialized signal is sent.
	}
    
	/*
	 * Return path to tiff file containing a particular slice
	 */
	public static File getOctreeFilePath(TileIndex tileIndex, TileFormat tileFormat, boolean zOriginNegativeShift) 
	{
		int axIx = tileIndex.getSliceAxis().index();
        
		File path = new File("");
		int octreeDepth = tileFormat.getZoomLevelCount();
		int depth = computeDepth(octreeDepth, tileIndex);
        if (depth < 0 || depth > octreeDepth) {
            // This situation can happen in production, owing to missing tiles.
            return null;
        }
        int[] xyz = null;
		xyz = new int[] {tileIndex.getX(), tileIndex.getY(), tileIndex.getZ()};
        
		// ***NOTE Raveler Z is slice count, not tile count***
        // so divide by tile Z dimension, to make z act like x and y
        xyz[axIx] = xyz[axIx] / tileFormat.getTileSize()[axIx];
        // and divide by zoom scale
        xyz[axIx] = xyz[axIx] / (int) Math.pow(2, tileIndex.getZoom());

        // start at lowest zoom to build up octree coordinates
		for (int d = 0; d < (depth - 1); ++d) {
			// How many Raveler tiles per octant at this zoom?
			int scale = (int)(Math.pow(2, depth - 2 - d)+0.1);
			int ds[] = {
					xyz[0]/scale,
					xyz[1]/scale,
					xyz[2]/scale};

            // Each dimension makes a binary contribution to the 
			// octree index.
			// Watch for illegal values
			// int ds[] = {dx, dy, dz};
			boolean indexOk = true;
			for (int index : ds) {
				if (index < 0)
					indexOk = false;
				if (index > 1)
					indexOk = false;
			}
			if (! indexOk) {
				log.error("Bad tile index "+tileIndex);
				return null;
			}
			// offset x/y/z for next deepest level
			for (int i = 0; i < 3; ++i)
				xyz[i] = xyz[i] % scale;

            // Octree coordinates are in z-order
			int octreeCoord = 1 + ds[0]
					// TODO - investigate possible ragged edge problems
					+ 2*(1 - ds[1]) // Raveler Y is at bottom; octree Y is at top
					+ 4*ds[2];

			path = new File(path, ""+octreeCoord);
		}
		return path;
	}
	
	@Override
	public TextureData2dGL loadToRam(TileIndex tileIndex)
			throws TileLoadError, MissingTileException 
	{
        return loadToRam(tileIndex, true);
	}

	public TextureData2dGL loadToRam(TileIndex tileIndex, boolean zOriginNegativeShift)
			throws TileLoadError, MissingTileException 
	{
		// Create a local load timer to measure timings just in this thread
		LoadTimer localLoadTimer = new LoadTimer();
		localLoadTimer.mark("starting slice load");
        final File octreeFilePath = getOctreeFilePath(tileIndex, tileFormat, zOriginNegativeShift);        
        if (octreeFilePath == null) {
            return null;
        }
		// TODO - generalize to URL, if possible
		// (though TIFF requires seek, right?)
		// Compute octree path from Raveler-style tile indices
		File folder = new File(topFolder, octreeFilePath.toString());
        
		// TODO for debugging, show file name for X tiles
		// Compute local z slice
		int zoomScale = (int)Math.pow(2, tileIndex.getZoom());
		int axisIx = tileIndex.getSliceAxis().index();
		int tileDepth = tileFormat.getTileSize()[axisIx];
		int absoluteSlice = (tileIndex.getCoordinate(axisIx)) / zoomScale;
		int relativeSlice = absoluteSlice % tileDepth;
		// Raveller y is flipped so flip when slicing in Y (right?)
		if (axisIx == 1)
			relativeSlice = tileDepth - relativeSlice - 1;
		
        // Calling this with "true" means I, the caller, accept that the array
        // returned may have one or more nulls in it.
		ImageDecoder[] decoders = createImageDecoders(folder, tileIndex.getSliceAxis(), true);
		
		// log.info(tileIndex + "" + folder + " : " + relativeSlice);
		TextureData2dGL result = loadSlice(relativeSlice, decoders);
		localLoadTimer.mark("finished slice load");

		loadTimer.putAll(localLoadTimer);
		return result;
	}

	public TextureData2dGL loadSlice(int relativeZ, ImageDecoder[] decoders) 
	throws TileLoadError 
    {
		int sc = tileFormat.getChannelCount();
		// 2 - decode image
		RenderedImage channels[] = new RenderedImage[sc];
        boolean emptyChannel = false;
        for (int c = 0; c < sc; ++c) {
            if (decoders[c] == null)
                emptyChannel = true;
        }
        if (emptyChannel) {
            return null;
        }
        else {
            for (int c = 0; c < sc; ++c) {
                try {
                    ImageDecoder decoder = decoders[c];
                    assert (relativeZ < decoder.getNumPages());
                    channels[c] = decoder.decodeAsRenderedImage(relativeZ);
                } catch (IOException e) {
                    throw new TileLoadError(e);
                }
                // localLoadTimer.mark("loaded slice, channel "+c);
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

            TextureData2dGL result = null;
            // My texture wrapper implementation
            TextureData2dGL tex = new TextureData2dGL();
            tex.loadRenderedImage(composite);
            result = tex;
            return result;
        }
	}

	// TODO - cache decoders if folder has not changed
	public ImageDecoder[] createImageDecoders(File folder, CoordinateAxis axis)
			throws MissingTileException, TileLoadError 
	{
		return createImageDecoders(folder, axis, false);
	}
	
	public ImageDecoder[] createImageDecoders(File folder, CoordinateAxis axis, boolean acceptNullDecoders)
			throws MissingTileException, TileLoadError 
	{
		String tiffBase = "default"; // Z-view; XY plane
		if (axis == CoordinateAxis.Y)
			tiffBase = "ZX";
		else if (axis == CoordinateAxis.X)
			tiffBase = "YZ";
		int sc = tileFormat.getChannelCount();
		ImageDecoder decoders[] = new ImageDecoder[sc];
        StringBuilder missingTiffs = new StringBuilder();
        StringBuilder requestedTiffs = new StringBuilder();
		for (int c = 0; c < sc; ++c) {
			File tiff = new File(folder, tiffBase+"."+c+".tif");
            if ( requestedTiffs.length() > 0 ) {
                requestedTiffs.append("; ");
            }
            requestedTiffs.append(tiff);
			if (! tiff.exists()) {
                if ( acceptNullDecoders ) {
                    if ( missingTiffs.length() > 0 ) {
                        missingTiffs.append(", ");
                    }
                    missingTiffs.append( tiff );
                }
                else {
    				throw new MissingTileException("Putative tiff file: " + tiff);
                }
            }
            else {
                try {
                    boolean useUrl = false;
                    if (useUrl) { // So SLOW
                        // test URL stream vs (seekable) file stream
                        URL url = tiff.toURI().toURL();
                        InputStream inputStream = url.openStream();
                        decoders[c] = ImageCodec.createImageDecoder("tiff", inputStream, null);
                    } else {
                        SeekableStream s = new FileSeekableStream(tiff);
                        decoders[c] = ImageCodec.createImageDecoder("tiff", s, null);
                    }
                } catch (IOException e) {
                    throw new TileLoadError(e);
                }
            }
		}
        if ( missingTiffs.length() > 0 ) {
            log.debug("All requested tiffs: " + requestedTiffs);
            log.debug( "Putative tiff file(s): " + missingTiffs + " not found.  Padding with zeros." );
        }
		return decoders;
	}
    
    private boolean sniffOriginAndScaleFromFolder(int [] origin, double [] scale) {
        File transformFile = new File(getTopFolder(), "transform.txt");
        if (! transformFile.exists())
            return false;
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
        } 
        catch (FileNotFoundException ex) {} 
        
        return false;
    }
    
    // Workaround for loading origin and scale without web services
    private void sniffOriginAndScale() throws DataSourceInitializeException {
        int [] origin = new int [3];
        double [] scale = new double [3];
        try {
            CoordinateToRawTransform transform
                    = ModelMgr.getModelMgr().getCoordToRawTransform(remoteBasePath);
            origin = transform.getOrigin();
            scale = transform.getScale();
        } catch ( Exception ex ) {
            if (! sniffOriginAndScaleFromFolder(origin, scale))
                throw new DataSourceInitializeException(
                        "Failed to find metadata", ex
                );
        }
        // Scale must be converted to micrometers.
        for (int i = 0; i < scale.length; i++) {
            scale[ i] /= 1000; // nanometers to micrometers
        }
        // Origin must be divided by 1000, to convert to micrometers.
        for (int i = 0; i < origin.length; i++) {
            origin[ i] = (int) (origin[i] /(1000 * scale[i])); // nanometers to voxels
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
    
	protected void sniffMetadata(File topFolderParam) 
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
            tileFormat.setHasZSlices(new File(topFolderParam, "default.0.tif").exists());

            // Deduce octree depth from directory structure depth
            int octreeDepth = 0;
            File deepFolder = topFolderParam;
            File deepFile = new File(topFolderParam, "default.0.tif");
            while (deepFile.exists()) {
                octreeDepth += 1;
                File parentFolder = deepFolder;
                // Check all possible children: some might be empty
                for (int branch = 1; branch <= 8; ++branch) {
                    deepFolder = new File(parentFolder, "" + branch);
                    deepFile = new File(deepFolder, "default.0.tif");
                    if (deepFile.exists()) {
                        break; // found a deeper branch
                    }
                }
            }
            int zoomFactor = (int) Math.pow(2, octreeDepth - 1);
            tileFormat.setZoomLevelCount(octreeDepth);

            // Deduce other parameters from first image file contents
            File tiff = new File(topFolderParam, "default.0.tif");
            SeekableStream s = new FileSeekableStream(tiff);
            ImageDecoder decoder = ImageCodec.createImageDecoder("tiff", s, null);
            // Z dimension is related to number of tiff pages
            int sz = decoder.getNumPages();

            // Get X/Y dimensions from first image
            RenderedImageAdapter ria = new RenderedImageAdapter(
                    decoder.decodeAsRenderedImage(0));
            int sx = ria.getWidth();
            int sy = ria.getHeight();
            // Full volume could be much larger than this downsampled tile
            int[] tileSize = new int[3];
            tileSize[2] = sz;
            if (sz < 1) {
                return;
            }
            tileSize[0] = sx;
            tileSize[1] = sy;
            tileFormat.setTileSize( tileSize );

            int[] volumeSize = new int[3];
            volumeSize[2] = zoomFactor * sz;
            volumeSize[0] = zoomFactor * sx;
            volumeSize[1] = zoomFactor * sy;
            tileFormat.setVolumeSize( volumeSize );

            int bitDepth = ria.getColorModel().getPixelSize();
            tileFormat.setBitDepth(bitDepth);
            tileFormat.setIntensityMax((int)Math.pow(2, bitDepth) - 1);

            tileFormat.setSrgb(ria.getColorModel().getColorSpace().isCS_sRGB());

            // Setup the origin and the scale.
            if (remoteBasePath != null) {
                sniffOriginAndScale();
            }
    		// TODO - actual max intensity
        
        } catch ( Exception ex ) {
            throw new DataSourceInitializeException(
                    "Failed to find metadata", ex
            );
        }
	}

    private static int computeDepth(int octreeDepth, TileIndex tileIndex) {
        return octreeDepth - tileIndex.getZoom();
    }
	
}
