package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;

/*
 * Common metadata to back end tile data formats
 */
public class TileFormat 
{
	private int[] origin = {0,0,0};
	private int[] volumeSize = {0,0,0};
	private int[] tileSize = {1024, 1024, 1};
	private double[] voxelMicrometers = {1,1,1};
	private int zoomLevelCount = 0;
	private int bitDepth = 8;
	private int channelCount = 1;
	private int intensityMax = 255;
	private int intensityMin = 0;
	private boolean srgb = false;
	private TileIndex.IndexStyle indexStyle = TileIndex.IndexStyle.QUADTREE;
	private boolean hasZSlices = true;
	private boolean hasXSlices = false;
	private boolean hasYSlices = false;

	public TileFormat() 
	{
		setDefaultParameters();
	}
	
	// new methods to help convert between TileIndex and xyz micrometer coordinates
	/**
	 * NOTE - it is possible for tileIndexForXyz to return 
	 * invalid TileIndexes, for example when the xyz coordinate lies
	 * outside the volume.
	 * 
	 * @param xyz
	 * @param zoom
	 * @param sliceDirection
	 * @return
	 */
	public TileIndex tileIndexForXyz(Vec3 xyz, int zoom, CoordinateAxis sliceDirection)
	{
		int zoomMax = getZoomLevelCount() - 1;
		Vec3 pixels = xyz.clone();
		double zoomFactor = Math.pow(2.0, zoom);
		int depthAxis = sliceDirection.index();
		// To generalize Raveler format, invert vertical tile dimension
		int verticalAxis = (depthAxis + 2) % 3;
		for (int i = 0; i < 3; ++i) {
			double val = pixels.get(i);
			// convert micrometers to voxels
			val /= getVoxelMicrometers()[i];
			// shift to volume origin
			val -= origin[i];
			// invert vertical direction
			if (i == verticalAxis)
				val = volumeSize[i] - val;
			// convert voxels to tiles
			if (i != depthAxis) // but not in slice direction
				val /= getTileSize()[i];
			// scale by zoom level
			if ((i != depthAxis) || (indexStyle == TileIndex.IndexStyle.OCTREE))
				val /= zoomFactor;
			// store result
			pixels.set(i, val);
		}
		//
		TileIndex result = new TileIndex(
				(int)Math.floor(pixels.get(0)),
				(int)Math.floor(pixels.get(1)),
				(int)Math.floor(pixels.get(2)),
				zoom, zoomMax, indexStyle, sliceDirection);
		return result;
	}
	
	/**
	 * Returns four corners in Z order:
	 *   1-----2
	 *   |     |
	 *   |     |
	 *   3-----4
	 *   
	 * @param index
	 * @return
	 */
	public Vec3[] cornersForTileIndex(TileIndex index) {
		double zoomFactor = Math.pow(2.0, index.getZoom());
		Vec3 v1 = new Vec3(index.getX(), index.getY(), index.getZ());
		int depthAxis = index.getSliceAxis().index();
		// To generalize Raveler format, invert vertical tile dimension
		int verticalAxis = (depthAxis + 2) % 3;
		int horizontalAxis = (depthAxis + 1) % 3;
		Vec3 dv = new Vec3(); // diagonal vector across tile block
		for (int i = 0; i < 3; ++i) {
			dv.set(i, getTileSize()[i] * getVoxelMicrometers()[i]);
			double val = v1.get(i);
			// shift to center of slice
			if (i == depthAxis)
				val += 0.5;
			// scale by zoom level
			if ((i != depthAxis) || (indexStyle == TileIndex.IndexStyle.OCTREE))
				val *= zoomFactor;
			// convert tiles to voxels
			if (i != depthAxis)
				val *= getTileSize()[i];
			// invert vertical direction; tile origin is bottom left, image origin is top left
			if (i == verticalAxis)
				val = volumeSize[i] - val + getTileSize()[i];
			// Shift to world origin
			val += origin[i];
			// convert voxels to micrometers
			val *= getVoxelMicrometers()[i];
			// 
			v1.set(i, val);
		}
		
		Vec3 dw = new Vec3(0,0,0);
		dw.set(horizontalAxis, dv.get(horizontalAxis));
		Vec3 dh = new Vec3(0,0,0);
		dh.set(verticalAxis, dv.get(verticalAxis));

		Vec3[] result = new Vec3[4];
		result[0] = v1;
		result[1] = v1.plus(dw);
		result[2] = v1.plus(dh);
		result[3] = v1.plus(dh).plus(dw);
		
		return null;
	}
	
	public int zoomLevelForCameraZoom(double pixelsPerSceneUnit) 
	{
		// use slightly lower resolution in the interest of speed.
		final double zoomOffset = 0.5;
		// 
		double[] vm = getVoxelMicrometers();
		double maxRes = Math.min(vm[0], Math.min(vm[1], vm[2]));
		double voxelsPerPixel = 1.0 / (pixelsPerSceneUnit * maxRes);
		int zoomMax = getZoomLevelCount() - 1;
		int zoom = zoomMax; // default to very coarse zoom
		if (voxelsPerPixel > 0.0) {
			double topZoom = Math.log(voxelsPerPixel) / Math.log(2.0);
			zoom = (int)(topZoom + zoomOffset);
		}
		int zoomMin = 0;
		zoom = Math.max(zoom, zoomMin);
		zoom = Math.min(zoom, zoomMax);
		return zoom;
	}
	
	public TileIndex.IndexStyle getIndexStyle() {
		return indexStyle;
	}

	public void setIndexStyle(TileIndex.IndexStyle indexStyle) {
		this.indexStyle = indexStyle;
	}

	public int[] getOrigin() {return origin;}


	public int[] getTileSize() {
		return tileSize;
	}

	public int[] getVolumeSize() {
		return volumeSize;
	}

	public double[] getVoxelMicrometers() {
		return voxelMicrometers;
	}

	public int getZoomLevelCount() {
		return zoomLevelCount;
	}

	public int getBitDepth() {
		return bitDepth;
	}

	public int getChannelCount() {
		return channelCount;
	}

	public int getIntensityMax() {
		return intensityMax;
	}

	public int getIntensityMin() {
		return intensityMin;
	}

	public boolean isSrgb() {
		return srgb;
	}

	public void setOrigin(int[] origin) {
		this.origin = origin;
	}

	public void setVolumeSize(int[] volumeSize) {
		this.volumeSize = volumeSize;
	}

	public void setTileSize(int[] tileSize) {
		this.tileSize = tileSize;
	}

	public void setVoxelMicrometers(double[] voxelMicrometers) {
		this.voxelMicrometers = voxelMicrometers;
	}

	public void setZoomLevelCount(int zoomLevelCount) {
		this.zoomLevelCount = zoomLevelCount;
	}

	public void setBitDepth(int bitDepth) {
		this.bitDepth = bitDepth;
	}

	public void setChannelCount(int channelCount) {
		this.channelCount = channelCount;
	}

	public void setIntensityMax(int intensityMax) {
		this.intensityMax = intensityMax;
	}

	public void setIntensityMin(int intensityMin) {
		this.intensityMin = intensityMin;
	}

	public void setDefaultParameters() {
		for (int i = 0; i < 3; ++i) {
			origin[i] = 0;
			volumeSize[i] = 512; // whatever
			tileSize[i] = 512; // X, Y, but not Z...
			voxelMicrometers[i] = 1.0;
		}
		tileSize[2] = 1; // tiles are 512x512x1
		zoomLevelCount = 1; // like small images
		bitDepth = 8;
		channelCount = 3; // rgb
		intensityMin = 0;
		intensityMax = 255;
		srgb = false;
		hasXSlices = false;
		hasYSlices = false;
		hasZSlices = true;
	}

	public void setSrgb(boolean srgb) {
		this.srgb = srgb;
	}

	public int getTileBytes() {
		int w = getTileSize()[0];
		int h = getTileSize()[1];
		int bpp = getChannelCount() * getBitDepth() / 8;
		return w * h * bpp;
	}

	public boolean isHasZSlices() {
		return hasZSlices;
	}

	public boolean isHasXSlices() {
		return hasXSlices;
	}

	public boolean isHasYSlices() {
		return hasYSlices;
	}

	public void setHasZSlices(boolean hasZSlices) {
		this.hasZSlices = hasZSlices;
	}

	public void setHasXSlices(boolean hasXSlices) {
		this.hasXSlices = hasXSlices;
	}

	public void setHasYSlices(boolean hasYSlices) {
		this.hasYSlices = hasYSlices;
	}

}
