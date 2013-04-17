package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedImageAdapter;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.SeekableStream;

/*
 * Loader for slice viewer format negotiated with Nathan Clack
 * March 21, 2013.
 * 512x512 tiles
 * Z-order octree folder layout
 * uncompressed tiff stack for each set of slices
 * named like "default.0.tif" for channel zero
 * 16-bit unsigned int
 * intensity range 0-65535
 */
public class BlockTiffOctreeLoadAdapter 
extends PyramidTextureLoadAdapter 
{
	// private static final Logger log = LoggerFactory.getLogger(BlockTiffOctreeLoadAdapter.class);

	// Metadata
	private File topFolder;
	public LoadTimer loadTimer = new LoadTimer();
	
	public BlockTiffOctreeLoadAdapter()
	{
		getTextureCache().setIndexStyle(TextureCache.IndexStyle.OCTREE);
		// Report performance statistics when program closes
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				loadTimer.report();
			}
		});
	}
	
	public File getTopFolder() {
		return topFolder;
	}

	public void setTopFolder(File topFolder) 
	throws IOException
	{
		this.topFolder = topFolder;
		sniffMetadata(topFolder);
		// Don't launch pre-fetch yet.
		// That must occur AFTER volume initialized signal is sent.
	}

	protected List<Integer> getOctreePath(PyramidTileIndex tileIndex) {
		Vector<Integer> result = new Vector<Integer>();
		
		int octreeDepth = tileFormat.getZoomLevelCount();
		int depth = octreeDepth - tileIndex.getZoom();
		assert(depth >= 0);
		assert(depth <= octreeDepth);
		// x and y are already corrected for tile size
		int x = tileIndex.getX();
		int y = tileIndex.getY();
		// ***NOTE Raveler Z is slice count, not tile count***
		int z = tileIndex.getZ() / tileFormat.getTileSize()[2];
		// start at lowest zoom to build up octree coordinates
		for (int d = 0; d < (depth - 1); ++d) {
			// How many Raveler tiles per octant at this zoom?
			int scale = (int)(Math.pow(2, d)+0.1);
			int dx = x / scale;
			int dy = y / scale;
			int dz = z / scale;
			// Each dimension makes a binary contribution to the 
			// octree index.
			assert(dx >= 0);
			assert(dy >= 0);
			assert(dz >= 0);
			assert(dx <= 1);
			assert(dy <= 1);
			assert(dz <= 1);
			// offset x/y/z for next deepest level
			x = x % scale;
			y = y % scale;
			z = z % scale;
			// Octree coordinates are in z-order
			int octreeCoord = 1 + dx 
					// TODO - investigate possible ragged edge problems
					+ 2*(1 - dy) // Raveler Y is at bottom; octree Y is at top
					+ 4*dz;
			result.add(octreeCoord);
		}
		
		return result;
	}
	
	/*
	 * Return path to tiff file containing a particular slice
	 */
	protected File getFilePath(PyramidTileIndex tileIndex) {
		File path = new File("");
		int octreeDepth = tileFormat.getZoomLevelCount();
		int depth = octreeDepth - tileIndex.getZoom();
		assert(depth >= 0);
		assert(depth <= octreeDepth);
		// x and y are already corrected for tile size and zoom level
		int x = tileIndex.getX();
		int y = tileIndex.getY();
		// ***NOTE Raveler Z is slice count, not tile count***
		// so divide by tile Z dimension, to make z act like x and y
		int z = tileIndex.getZ() / tileFormat.getTileSize()[2];
		// and divide by zoom scale
		z = z / (int)Math.pow(2, tileIndex.getZoom());
		// start at lowest zoom to build up octree coordinates
		for (int d = 0; d < (depth - 1); ++d) {
			// How many Raveler tiles per octant at this zoom?
			int scale = (int)(Math.pow(2, depth - 2 - d)+0.1);
			int dx = x / scale;
			int dy = y / scale;
			int dz = z / scale;
			// Each dimension makes a binary contribution to the 
			// octree index.
			// Watch for illegal values
			int ds[] = {dx, dy, dz};
			boolean indexOk = true;
			for (int index : ds) {
				if (index < 0)
					indexOk = false;
				if (index > 1)
					indexOk = false;
			}
			if (! indexOk) {
				System.out.println("Bad tile index "+tileIndex);
				return null;
			}
			assert(dx >= 0);
			assert(dy >= 0);
			assert(dz >= 0);
			assert(dx <= 1);
			assert(dy <= 1);
			assert(dz <= 1);
			// offset x/y/z for next deepest level
			x = x % scale;
			y = y % scale;
			z = z % scale;
			// Octree coordinates are in z-order
			int octreeCoord = 1 + dx 
					// TODO - investigate possible ragged edge problems
					+ 2*(1 - dy) // Raveler Y is at bottom; octree Y is at top
					+ 4*dz;

			path = new File(path, ""+octreeCoord);
		}
		return path;
	}
	
	@Override
	public TextureData2dGL loadToRam(PyramidTileIndex tileIndex)
			throws TileLoadError, MissingTileException 
	{
		// Create a local load timer to measure timings just in this thread
		LoadTimer localLoadTimer = new LoadTimer();
		localLoadTimer.mark("starting slice load");
		// TODO - generalize to URL, if possible
		// (though TIFF requires seek, right?)
		// Compute octree path from Raveler-style tile indices
		File folder = new File(topFolder, getFilePath(tileIndex).toString());
		int sc = tileFormat.getChannelCount();
		// Compute local z slice
		int zoomScale = (int)Math.pow(2, tileIndex.getZoom());
		int tileDepth = tileFormat.getTileSize()[2];
		int relativeZ = (tileIndex.getZ() / zoomScale) % tileDepth;
		RenderedImage channels[] = new RenderedImage[sc];
		for (int c = 0; c < sc; ++c) {
			File tiff = new File(folder, "default."+c+".tif");
			// System.out.println(tileIndex+", "+tiff.toString());
			if (! tiff.exists())
				throw new MissingTileException();
			try {
				SeekableStream s = new FileSeekableStream(tiff);
				ImageDecoder decoder = ImageCodec.createImageDecoder("tiff", s, null);
				assert(relativeZ < decoder.getNumPages());
				channels[c] = decoder.decodeAsRenderedImage(relativeZ);
			} catch (IOException e) {
				throw new TileLoadError(e);
			}
			localLoadTimer.mark("loaded slice, channel "+c);
		}
		// Combine channels into one image
		RenderedImage composite = channels[0];
		if (sc > 1) {
			ParameterBlockJAI pb = new ParameterBlockJAI("bandmerge");
			for (int c = 0; c < sc; ++c)
				pb.addSource(channels[c]);
			composite = JAI.create("bandmerge", pb);
			localLoadTimer.mark("merged channels");
		}
		
		TextureData2dGL result = null;
		// My texture wrapper implementation
		TextureData2dGL tex = new TextureData2dGL();
		tex.loadRenderedImage(composite);
		result = tex;

		loadTimer.putAll(localLoadTimer);
		return result;
	}
	
	protected void sniffMetadata(File topFolderParam) 
	throws IOException
	{
		// Set some default parameters, to be replaced my measured parameters
		tileFormat.setDefaultParameters();

		// Count color channels by counting channel files
		tileFormat.setChannelCount(0);
		int channelCount = 0;
		while (true) {
			File tiff = new File(topFolderParam, "default."+channelCount+".tif");
			if (! tiff.exists())
				break;
			channelCount += 1;
		}
		tileFormat.setChannelCount(channelCount);
		if (channelCount < 1)
			return;
		
		// Deduce octree depth from directory structure depth
		int octreeDepth = 0;
		File deepFolder = topFolderParam;
		File deepFile = new File(topFolderParam, "default.0.tif");
		while (deepFile.exists()) {
			octreeDepth += 1;
			File parentFolder = deepFolder;
			// Check all possible children: some might be empty
			for (int branch = 1; branch <= 8; ++branch) {
				deepFolder = new File(parentFolder, ""+branch);
				deepFile = new File(deepFolder, "default.0.tif");
				if (deepFile.exists())
					break; // found a deeper branch
			}
		}
		int zoomFactor = (int)Math.pow(2, octreeDepth - 1);
		tileFormat.setZoomLevelCount(octreeDepth);
		
		// Deduce other parameters from first image file contents
		File tiff = new File(topFolderParam, "default.0.tif");
		SeekableStream s = new FileSeekableStream(tiff);
		ImageDecoder decoder = ImageCodec.createImageDecoder("tiff", s, null);
		// Z dimension is related to number of tiff pages
		int sz = decoder.getNumPages();
		// Full volume could be much larger than this downsampled tile
		tileFormat.getVolumeSize()[2] = zoomFactor * sz;
		tileFormat.getTileSize()[2] = sz;
		if (sz < 1)
			return;
		
		// Get X/Y dimensions from first image
		RenderedImageAdapter ria = new RenderedImageAdapter(
				decoder.decodeAsRenderedImage(0));
		int sx = ria.getWidth();
		int sy = ria.getHeight();
		tileFormat.getVolumeSize()[0] = zoomFactor * sx;
		tileFormat.getVolumeSize()[1] = zoomFactor * sy;
		tileFormat.getTileSize()[0] = sx;
		tileFormat.getTileSize()[1] = sy;

		int bitDepth = ria.getColorModel().getPixelSize();
		tileFormat.setBitDepth(bitDepth);
		tileFormat.setIntensityMax((int)Math.pow(2, bitDepth));

		tileFormat.setSrgb(ria.getColorModel().getColorSpace().isCS_sRGB());
		
		// TODO - actual max intensity
		// TODO - voxel size in micrometers
	}


}
