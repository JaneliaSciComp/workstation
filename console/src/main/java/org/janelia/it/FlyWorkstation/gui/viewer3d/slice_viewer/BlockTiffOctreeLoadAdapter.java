package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import javax.media.jai.RenderedImageAdapter;

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
	File topFolder;
	
	public BlockTiffOctreeLoadAdapter(
			File topFolder) 
	{
		setTopFolder(topFolder);
	}
	
	public File getTopFolder() {
		return topFolder;
	}

	public void setTopFolder(File topFolder) {
		this.topFolder = topFolder;
		// TODO populate tileFormat
		// hard-code for the moment... TODO
		tileFormat.setBitDepth(16);
		tileFormat.setChannelCount(1); // TODO
		tileFormat.setIntensityMax(65535);
		int tileSize[] = {340, 494, 171};
		tileFormat.setTileSize(tileSize);
		int volumeSize[] = {680, 988, 342};
		tileFormat.setVolumeSize(volumeSize);
		double voxelMicrometers[] = {1.0, 1.0, 1.0}; // TODO
		tileFormat.setVoxelMicrometers(voxelMicrometers);
		tileFormat.setZoomLevelCount(2);
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
	public PyramidTextureData loadToRam(PyramidTileIndex tileIndex)
			throws TileLoadError, MissingTileException 
	{
		// TODO - generalize to URL, if possible
		// (though TIFF requires seek, right?)
		// Compute octree path from Raveler-style tile indices
		File folder = new File(topFolder, getFilePath(tileIndex).toString());
		int sc = tileFormat.getChannelCount();
		// Compute local z slice
		int zoomScale = (int)Math.pow(2, tileIndex.getZoom());
		int tileDepth = tileFormat.getTileSize()[2];
		int relativeZ = (tileIndex.getZ() / zoomScale) % tileDepth;
		BufferedImage channels[] = new BufferedImage[sc];
		for (int c = 0; c < sc; ++c) {
			File tiff = new File(folder, "default."+c+".tif");
			System.out.println(tileIndex+", "+tiff.toString());
			if (! tiff.exists())
				throw new MissingTileException();
			try {
				SeekableStream s = new FileSeekableStream(tiff);
				ImageDecoder decoder = ImageCodec.createImageDecoder("tiff", s, null);
				assert(relativeZ < decoder.getNumPages());
				RenderedImageAdapter ria = new RenderedImageAdapter(
						decoder.decodeAsRenderedImage(relativeZ));
				channels[c] = ria.getAsBufferedImage();				
			} catch (IOException e) {
				throw new TileLoadError(e);
			}
		}
		// TODO combine channels into one image
		// For now just use the first channel...
		return convertToGlFormat(channels[0]);
	}

}
