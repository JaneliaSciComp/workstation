package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import org.janelia.it.FlyWorkstation.geom.CoordinateAxis;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;

/*
 * Common metadata to back end tile data formats
 */
public class TileFormat 
{

	private int[] origin = {0,0,0}; // in voxel
	private int[] volumeSize = {0,0,0}; // in voxels
	private int[] tileSize = {1024, 1024, 1}; // in voxels (possibly)
	private double[] voxelMicrometers = {1,1,1}; // in micrometers per voxel
	private int zoomLevelCount = 0;
	private double zoomFactorCache[] = {1, 2, 4, 8};
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
		// New way
		MicrometerXyz um = new MicrometerXyz(xyz.getX(), xyz.getY(), xyz.getZ());
		VoxelXyz vox = voxelXyzForMicrometerXyz(um);
		ZoomedVoxelXyz zvox = zoomedVoxelXyzForVoxelXyz(vox, zoom, sliceDirection);
		TileXyz tileXyz = tileXyzForZoomedVoxelXyz(zvox, zoom, sliceDirection);
		return new TileIndex(
				tileXyz.getX(), tileXyz.getY(), tileXyz.getZ(),
				zoom, zoomMax, indexStyle, sliceDirection);
	}
	
	public BoundingBox3d calcBoundingBox() {
		double sv[] = getVoxelMicrometers();
		int s0[] = getOrigin();
		int s1[] = getVolumeSize();
		Vec3 b0 = new Vec3(sv[0]*s0[0], sv[1]*s0[1], sv[2]*s0[2]);
		Vec3 b1 = new Vec3(sv[0]*(s0[0]+s1[0]), sv[1]*(s0[1]+s1[1]), sv[2]*(s0[2]+s1[2]));
		BoundingBox3d result = new BoundingBox3d();
		result.setMin(b0);
		result.setMax(b1);
		return result;
	}
	
	/**
	 * Returns four corner locations in units of micrometers, relative to
	 * the full parent volume, in Z order:
	 * <pre>
	 *   0-----1       x--->
	 *   |     |    y
	 *   |     |    |
	 *   2-----3    v
	 * </pre>
	 * 
	 * NOTE: The behavior of this method for non-Z slices probably requires more
	 * thought and testing.
	 * 
	 * @param index
	 * @return
	 */
	public Vec3[] cornersForTileIndex(TileIndex index) {
		// New way
		// upper left front corner of tile
		TileXyz tileXyz = new TileXyz(index.getX(), index.getY(), index.getZ());
		ZoomedVoxelXyz zvox = zoomedVoxelXyzForTileXyz(tileXyz, index.getZoom(), index.getSliceAxis());
		VoxelXyz vox = voxelXyzForZoomedVoxelXyz(zvox, index.getZoom(), index.getSliceAxis());
		MicrometerXyz ulfCorner = micrometerXyzForVoxelXyz(vox, index.getSliceAxis());
		// lower right back corner
		int dt[] = {1, -1, 1}; // shift by one tile to get opposite corner
		int depthAxis = index.getSliceAxis().index();
		dt[depthAxis] = 0; // but no shift in slice direction
		TileXyz tileLrb = new TileXyz(index.getX() + dt[0], index.getY() + dt[1], index.getZ() + dt[2]);
		ZoomedVoxelXyz zVoxLrb = zoomedVoxelXyzForTileXyz(tileLrb, index.getZoom(), index.getSliceAxis());
		VoxelXyz voxLrb = voxelXyzForZoomedVoxelXyz(zVoxLrb, index.getZoom(), index.getSliceAxis());
		MicrometerXyz lrbCorner = micrometerXyzForVoxelXyz(voxLrb, index.getSliceAxis());
		//
// Checking in commented code. Commented to avoid breaking Chris' other changes.
//		Vec3 dv = new Vec3(); // diagonal vector across tile block
//		for (int i = 0; i < 3; ++i) {
//			dv.set(i, zoomFactor * getTileSize()[i] * getVoxelMicrometers()[i]);
//			double val = v1.get(i);
//			// shift to center of slice
//			if (i == depthAxis)
//				val += 0.5;
//			// scale by zoom level
//			if ((i != depthAxis) || (indexStyle == TileIndex.IndexStyle.OCTREE))
//				val *= zoomFactor;
//			// convert tiles to voxels
//			if (i != depthAxis)
//				val *= getTileSize()[i];
//			// Shift to world origin
//			val += origin[i];
//			// convert voxels to micrometers
//			val *= getVoxelMicrometers()[i];
//			//
//			v1.set(i, val);
//		}
		Vec3 ulf = new Vec3(ulfCorner.getX(), ulfCorner.getY(), ulfCorner.getZ());
		Vec3 lrb = new Vec3(lrbCorner.getX(), lrbCorner.getY(), lrbCorner.getZ());
		Vec3 dv = lrb.minus(ulf);
		// To generalize Raveler format, invert vertical tile dimension
		int verticalAxis = (depthAxis + 2) % 3;
		int horizontalAxis = (depthAxis + 1) % 3;
		Vec3 dw = new Vec3(0,0,0);
		dw.set(horizontalAxis, dv.get(horizontalAxis));
		Vec3 dh = new Vec3(0,0,0);
		dh.set(verticalAxis, dv.get(verticalAxis));

		Vec3[] result = new Vec3[4];
		result[0] = ulf;
		result[1] = ulf.plus(dw);
		result[2] = ulf.plus(dh);
		result[3] = ulf.plus(dh).plus(dw);
		
		return result;
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

	/**
	 * Upper left front corner of volume, in units of voxels
	 * @return
	 */
	public int[] getOrigin() {return origin;}


	/**
	 * (maximum) Size of individual tiles, in units of voxels.
	 * Includes extent in all X/Y/Z directions, even though any particular
	 * 2D tile will extend in only two dimensions.
	 * Tiles on the right/upper/front edges of some volumes might be less than 
	 * this full tile size, if the total volume size is not a multiple of the 
	 * tile size.
 	 * @return
	 */
	public int[] getTileSize() {
		return tileSize;
	}

	/**
	 * Total X/Y/Z size of the represented volume, in units of voxels.
	 * @return
	 */
	public int[] getVolumeSize() {
		return volumeSize;
	}

	/**
	 * X/Y/Z size of one voxel, in units of micrometers per voxel.
	 * @return
	 */
	public double[] getVoxelMicrometers() {
		return voxelMicrometers;
	}

	/**
	 * Number of power-of-two zoom levels available for this zoomable volume
	 * representation.
	 * @return
	 */
	public int getZoomLevelCount() {
		return zoomLevelCount;
	}

	/**
	 * Number of bits of image intensity information, per color channel.
	 * Should be 8 or 16 (?).
	 * @return
	 */
	public int getBitDepth() {
		return bitDepth;
	}

	/**
	 * Number of color channels in image data.
	 * @return
	 */
	public int getChannelCount() {
		return channelCount;
	}

	/**
	 * Largest intensity in the image. For example, for 12-bit data in a 16-bit
	 * container, getBitDepth() == 16, and getIntensityMax() == 4095.0.
	 * @return
	 */
	public int getIntensityMax() {
		return intensityMax;
	}

	/**
	 * Smallest intensity in the image. Usually zero.
	 * (or one? Zero means "missing" or "invalid")
	 * @return
	 */
	public int getIntensityMin() {
		return intensityMin;
	}

	/**
	 * Whether the image intensity data are already pre-corrected for display
	 * on a computer monitor. Usually false, meaning that the intensities
	 * are linearly related to luminous intensity.
	 * @return
	 */
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
		if (this.zoomLevelCount == zoomLevelCount)
			return; // no change
		this.zoomLevelCount = zoomLevelCount;
		// Avoid calling Math.pow() every single time
		zoomFactorCache = new double[zoomLevelCount];
		for (int z = 0; z < zoomLevelCount; ++z)
			zoomFactorCache[z] = Math.pow(2, z);
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

	/**
	 * Populates an initial non-pathological set of image parameters:
	 *   origin = [0,0,0]
	 *   volumeSize = [512,512,512]
	 *   tileSize = [512,512,1]
	 *   zoomLevelCount = 1
	 *   bitDepth = 8
	 *   channelCount = 3
	 *   intensityMin = 0
	 *   intensityMax = 255
	 *   sRgb = false
	 */
	public void setDefaultParameters() {
		for (int i = 0; i < 3; ++i) {
			origin[i] = 0;
			volumeSize[i] = 512; // whatever
			tileSize[i] = 512; // X, Y, but not Z...
			voxelMicrometers[i] = 1.0;
		}
		tileSize[2] = 1; // tiles are 512x512x1
		setZoomLevelCount(1); // like small images
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

	/**
	 * Helper to avoid calling Math.pow() all the time.
	 * @param zoomLevel
	 * @return
	 */
	protected double zoomFactorForZoomLevel(int zoomLevel) {
		if ((zoomLevel >= 0) && (zoomLevel < zoomLevelCount))
			return zoomFactorCache[zoomLevel];
		// Rare case where zoomLevel is outside acceptable range
		return Math.pow(2, zoomLevel);
	}
	
	// Aug 26, 2013 attempt at greater type safety between different x/y/z units
	/// Unit conversion methods:
	public MicrometerXyz micrometerXyzForVoxelXyz(VoxelXyz v, CoordinateAxis sliceDirection) {
		// return point at upper-left corner of voxel, but centered in slice direction
		double xyz[] = {
				v.getX() + origin[0],
				v.getY() + origin[1],
				v.getZ() + origin[2],
		};
		xyz[sliceDirection.index()] += 0.5;
		return new MicrometerXyz(
				xyz[0] * getVoxelMicrometers()[0],
				xyz[1]  * getVoxelMicrometers()[1],				
				xyz[2]  * getVoxelMicrometers()[2]);
	}
	public VoxelXyz voxelXyzForMicrometerXyz(MicrometerXyz m) {
		return new VoxelXyz(
				(int)Math.floor(m.getX() / getVoxelMicrometers()[0]) - origin[0],
				(int)Math.floor(m.getY() / getVoxelMicrometers()[1]) - origin[1],
				(int)Math.floor(m.getZ() / getVoxelMicrometers()[2]) - origin[2]);
	}
	
	public VoxelXyz voxelXyzForZoomedVoxelXyz(ZoomedVoxelXyz z, int zoomLevel, CoordinateAxis sliceAxis) {
		int zoomFactor = (int)zoomFactorForZoomLevel(zoomLevel);
		int xyz[] = {z.getX(), z.getY(), z.getZ()};
		int depthAxis = sliceAxis.index();
		for (int i = 0; i < 3; ++i) {
			if ( (i == depthAxis) && (indexStyle == TileIndex.IndexStyle.QUADTREE) )
				continue; // don't zoom on slice axis in quadtree mode
			xyz[i] *= zoomFactor;
		}
		return new VoxelXyz(xyz[0], xyz[1], xyz[2]);
	}
	public ZoomedVoxelXyz zoomedVoxelXyzForVoxelXyz(VoxelXyz v, int zoomLevel, CoordinateAxis sliceAxis) 
	{
		int zoomFactor = (int)zoomFactorForZoomLevel(zoomLevel);
		int xyz[] = {v.getX(), v.getY(), v.getZ()};
		int depthAxis = sliceAxis.index();
		for (int i = 0; i < 3; ++i) {
			if ( (i == depthAxis) && (indexStyle == TileIndex.IndexStyle.QUADTREE) )
				continue; // don't zoom on slice axis in quadtree mode
			xyz[i] /= zoomFactor;
		}
		return new ZoomedVoxelXyz(xyz[0], xyz[1], xyz[2]);
	}
	
	/**
	 * ZoomedVoxel at upper left front corner of tile
	 * @param t
	 * @param sliceAxis
	 * @return
	 */
	public ZoomedVoxelXyz zoomedVoxelXyzForTileXyz(TileXyz t, int zoomLevel, CoordinateAxis sliceAxis) {
		int xyz[] = {t.getX(), t.getY(), t.getZ()};
		int depthAxis = sliceAxis.index();
		for (int i = 0; i < 3; ++i) {
			if (i == depthAxis)
				continue; // Don't scale depth axis
			else
				xyz[i] = xyz[i]*getTileSize()[i]; // scale horizontal and vertical
		}
		// Invert Y axis to convert to Raveler convention from image convention.
		int zoomFactor = (int)zoomFactorForZoomLevel(zoomLevel);
		int maxZoomVoxelY = volumeSize[1] / zoomFactor;
		xyz[1] = maxZoomVoxelY - xyz[1] - getTileSize()[1];
		//
		return new ZoomedVoxelXyz(xyz[0], xyz[1], xyz[2]);
	}
	/**
	 * TileIndex xyz containing ZoomedVoxel
	 * @param z
	 * @param zoomLevel
	 * @param sliceAxis
	 * @return
	 */
	public TileXyz tileXyzForZoomedVoxelXyz(ZoomedVoxelXyz z, int zoomLevel, CoordinateAxis sliceAxis) {
		int xyz[] = {z.getX(), z.getY(), z.getZ()};
		// Invert Y axis to convert to Raveler convention from image convention.
		int zoomFactor = (int)zoomFactorForZoomLevel(zoomLevel);
		int maxZoomVoxelY = volumeSize[1] / zoomFactor - 1;
		xyz[1] = maxZoomVoxelY - xyz[1];
		int depthAxis = sliceAxis.index();
		for (int i = 0; i < 3; ++i) {
			if (i == depthAxis)
				continue; // Don't scale depth axis
			else
				xyz[i] = xyz[i]/getTileSize()[i]; // scale horizontal and vertical
		}
		return new TileXyz(xyz[0], xyz[1], xyz[2]);
	}
	
	// Volume units can be one of 4 interconvertible types
	// These classes are intended to enforce type safety between different unit types
	public static interface Unit {}; // Base unit
	public static class MicrometerUnit implements Unit {}; // 1 um
	public static class VoxelUnit implements Unit{}; // 2 voxel
	public static class ZoomedVoxelUnit implements Unit{}; // 3 zoomed voxel (fewer, larger voxels)
	public static class TileUnit implements Unit{}; // 4 tile index (Raveler-like tile index)
	
	public static class UnittedVec3Int<U extends Unit>
	implements Cloneable
	{
		private int data[] = new int[3];
		
		public UnittedVec3Int(int x, int y, int z) {
			data[0] = x;
			data[1] = y;
			data[2] = z;
		}
		
		@Override
		public UnittedVec3Int<U> clone() {
			return new UnittedVec3Int<U>(data[0], data[1], data[2]);
		}
		
		public int getX() {return data[0];}
		public int getY() {return data[1];}
		public int getZ() {return data[2];}
	}; 
	
	public static class UnittedVec3Double<U extends Unit> 
	implements Cloneable
	{
		private double data[] = new double[3];
		
		public UnittedVec3Double(double x, double y, double z) {
			data[0] = x;
			data[1] = y;
			data[2] = z;
		}
		
		@Override
		public UnittedVec3Double<U> clone() {
			return new UnittedVec3Double<U>(data[0], data[1], data[2]);
		}
		
		public double getX() {return data[0];}
		public double getY() {return data[1];}
		public double getZ() {return data[2];}
	}; 
	
	// Base
	public static class MicrometerXyz extends UnittedVec3Double<VoxelUnit>
	{
		public MicrometerXyz(double x, double y, double z) {super(x,y,z);}
	}; // 1	
	
	public static class VoxelXyz extends UnittedVec3Int<VoxelUnit> 
	{
		public VoxelXyz(int x, int y, int z) {super(x, y, z);}
	}; // 2
	
	public static class ZoomedVoxelXyz extends UnittedVec3Int<ZoomedVoxelUnit> 
	{		
		public ZoomedVoxelXyz(int x, int y, int z) {super(x, y, z);}
	}; // 3
	
	public static class TileXyz extends UnittedVec3Int<TileUnit> 
	{
		public TileXyz(int x, int y, int z) {super(x, y, z);}		
	}; // 4

}
