package org.janelia.it.workstation.gui.large_volume_viewer;

import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.octree.ZoomLevel;
import org.janelia.it.workstation.octree.ZoomedVoxelIndex;

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
	    ZoomLevel zoomLevel = new ZoomLevel(zoom); // TODO put in arg list
		int zoomMax = getZoomLevelCount() - 1;
		// New way
		MicrometerXyz um = new MicrometerXyz(xyz.getX(), xyz.getY(), xyz.getZ());
		VoxelXyz vox = voxelXyzForMicrometerXyz(um);
		ZoomedVoxelIndex zvox = zoomedVoxelIndexForVoxelXyz(vox, zoomLevel, sliceDirection);
		TileXyz tileXyz = tileXyzForZoomedVoxelIndex(zvox, sliceDirection);
		return new TileIndex(
				tileXyz.getX(), tileXyz.getY(), tileXyz.getZ(),
				zoom, zoomMax, indexStyle, sliceDirection);
	}
	
    public TileIndex tileIndexForZoomedVoxelIndex(ZoomedVoxelIndex ix,
            CoordinateAxis sliceDirection) 
    {
        int zoom = ix.getZoomLevel().getLog2ZoomOutFactor();
        TileXyz tileXyz = tileXyzForZoomedVoxelIndex(
                ix, 
                sliceDirection);
        int zoomMax = getZoomLevelCount() - 1;
        return new TileIndex(
                tileXyz.getX(), tileXyz.getY(), tileXyz.getZ(),
                zoom, zoomMax, indexStyle, sliceDirection);
    }

	public BoundingBox3d calcBoundingBox() {
        double sv[] = getVoxelMicrometers();
		int s0[] = getOrigin();
		int s1[] = getVolumeSize();
        
        // Eliminating voxel size multiplication from X coord: offset already
        // in voxel coordinates.
		Vec3 b0 = new Vec3(sv[0]*s0[0], sv[1]*s0[1], sv[2]*s0[2]);
		Vec3 b1 = new Vec3(sv[0]*(s0[0]+s1[0]), sv[1]*(s0[1]+s1[1]), sv[2]*(s0[2]+s1[2]));
		BoundingBox3d result = new BoundingBox3d();
		result.setMin(b0);
		result.setMax(b1);
		return result;
	}
    
// Refactored for standard calculations.    
//    public BoundingBox3d calcBoundingBox() {
//        double sv[] = getVoxelMicrometers();
//		int s0[] = getOrigin();
//		int s1[] = getVolumeSize();
//        
//        MicrometerXyz mmxMin = micrometerXyzForVoxelXyz(
//                new VoxelXyz(s0), CoordinateAxis.Z
//        );
//		MicrometerXyz mmxMax = micrometerXyzForVoxelXyz(
//                new VoxelXyz(
//                        s0[0] + s1[0],
//                        s0[1] + s1[1],
//                        s0[2] + s1[2]
//                ),
//                CoordinateAxis.Z
//        );
//		BoundingBox3d result = new BoundingBox3d();
//		result.setMin(mmxMin.getX(), mmxMin.getY(), mmxMin.getZ());
//		result.setMax(mmxMax.getX(), mmxMax.getY(), mmxMax.getZ());
//		return result;
//	}
	
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
	
    public TileBoundingBox screenBoundsToTileBounds(int[] xyzFromWhd, ScreenBoundingBox screenBounds, int zoom) {

        double zoomFactor = Math.pow(2.0, zoom);
		// get tile pixel size 1024 from loadAdapter
        double resolution0 = getVoxelMicrometers()[xyzFromWhd[0]];
        double resolution1 = getVoxelMicrometers()[xyzFromWhd[1]];
		double tileWidth = tileSize[xyzFromWhd[0]] * zoomFactor * resolution0;
		double tileHeight = tileSize[xyzFromWhd[1]] * zoomFactor * resolution1;

        //int xOrigin = getOrigin()[xyzFromWhd[0]];
		int wMin = (int)Math.floor((screenBounds.getwFMin()/* - xOrigin*/) / tileWidth);
		int wMax = (int)Math.floor((screenBounds.getwFMax()/* - xOrigin*/) / tileWidth);

		int hMin = (int)Math.floor(screenBounds.gethFMin() / tileHeight);
		int hMax = (int)Math.floor(screenBounds.gethFMax() / tileHeight);
        
        TileBoundingBox tileUnits = new TileBoundingBox();
        tileUnits.sethMax(hMax);
        tileUnits.sethMin(hMin);
        tileUnits.setwMax(wMax);
        tileUnits.setwMin(wMin);
        
        return tileUnits;
    }
    
    public ScreenBoundingBox boundingBoxToScreenBounds(
            BoundingBox3d bb, int viewWidth, int viewHeight, Vec3 focus, double pixelsPerSceneUnit, int[] xyzFromWhd 
    ) {
        
		double bottomY = bb.getMax().getY();

        // In scene units
		// Clip to screen space
		double wFMin = focus.get(xyzFromWhd[0]) - 0.5*viewWidth/pixelsPerSceneUnit;
		double wFMax = focus.get(xyzFromWhd[0]) + 0.5*viewWidth/pixelsPerSceneUnit;
		double hFMin = focus.get(xyzFromWhd[1]) - 0.5*viewHeight/pixelsPerSceneUnit;
		double hFMax = focus.get(xyzFromWhd[1]) + 0.5*viewHeight/pixelsPerSceneUnit;
		// Clip to volume space
		// Subtract one half pixel to avoid loading an extra layer of tiles
		double dw = 0.25 * getVoxelMicrometers()[xyzFromWhd[0]];
		double dh = 0.25 * getVoxelMicrometers()[xyzFromWhd[1]];
		wFMin = Math.max(wFMin, bb.getMin().get(xyzFromWhd[0]) + dw);
		hFMin = Math.max(hFMin, bb.getMin().get(xyzFromWhd[1]) + dh);
		wFMax = Math.min(wFMax, bb.getMax().get(xyzFromWhd[0]) - dw);
		hFMax = Math.min(hFMax, bb.getMax().get(xyzFromWhd[1]) - dh);
        
		// Correct for bottom Y origin of Raveler tile coordinate system
		// (everything else is top Y origin: image, our OpenGL, user facing coordinate system)
        final double bbMinY = bb.getMin().getY();
		if (xyzFromWhd[0] == 1) { // Y axis left-right
			double temp = wFMin;
			wFMin = bottomY - wFMax + bbMinY;
			wFMax = bottomY - temp + bbMinY;
		}
		else if (xyzFromWhd[1] == 1) { // Y axis top-bottom
			double temp = hFMin;
			hFMin = bottomY - hFMax + bbMinY;
			hFMax = bottomY - temp + bbMinY;
		}
		else {
			// TODO - invert slice axis? (already inverted above)
		}
        
        ScreenBoundingBox screenBoundaries = new ScreenBoundingBox();
        screenBoundaries.sethFMax(hFMax);
        screenBoundaries.sethFMin(hFMin);
        
        screenBoundaries.setwFMax(wFMax);
        screenBoundaries.setwFMin(wFMin);
        
        return screenBoundaries;
    }
    
    public int calcRelativeTileDepth(int[] xyzFromWhd, double focusDepth, BoundingBox3d bb) {
        // Bounding box is actually 0.5 voxels bigger than number of slices at each end
        int dMin = (int)(bb.getMin().get(xyzFromWhd[2])/getVoxelMicrometers()[xyzFromWhd[2]] + 0.5);
        int dMax = (int)(bb.getMax().get(xyzFromWhd[2])/getVoxelMicrometers()[xyzFromWhd[2]] - 0.5);
        int absoluteTileDepth = (int)Math.round(focusDepth / getVoxelMicrometers()[xyzFromWhd[2]] - 0.5);
        absoluteTileDepth = Math.max(absoluteTileDepth, dMin);
        absoluteTileDepth = Math.min(absoluteTileDepth, dMax);
        int relativeTileDepth = absoluteTileDepth - getOrigin()[xyzFromWhd[2]];
        return relativeTileDepth;
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
        // In case somewhere, the original array is being passed around
        // and used directly, prior to having been reset from defaults.
        if (this.volumeSize != null) {
            for (int i = 0; i < volumeSize.length; i++) {
                this.volumeSize[i] = volumeSize[i];
            }
        }
		this.volumeSize = volumeSize;
	}

	public void setTileSize(int[] tileSize) {
        // In case somewhere, the original array is being passed around
        // and used directly, prior to having been reset from defaults.
        if (this.tileSize != null) {
            for (int i = 0; i < tileSize.length; i++) {
                this.tileSize[i] = tileSize[i];
            }
        }
        else {
    		this.tileSize = tileSize;
        }
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
	public final void setDefaultParameters() {
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
	
	public VoxelXyz voxelXyzForZoomedVoxelIndex(ZoomedVoxelIndex z, CoordinateAxis sliceAxis) {
		int zoomFactor = z.getZoomLevel().getZoomOutFactor();
		int xyz[] = {z.getX(), z.getY(), z.getZ()};
		int depthAxis = sliceAxis.index();
		for (int i = 0; i < 3; ++i) {
			if ( (i == depthAxis) && (indexStyle == TileIndex.IndexStyle.QUADTREE) )
				continue; // don't zoom on slice axis in quadtree mode
			xyz[i] *= zoomFactor;
		}
		return new VoxelXyz(xyz[0], xyz[1], xyz[2]);
	}
	public ZoomedVoxelIndex zoomedVoxelIndexForVoxelXyz(VoxelXyz v, ZoomLevel zoomLevel, CoordinateAxis sliceAxis) 
	{
		int zoomFactor = zoomLevel.getZoomOutFactor();
		int xyz[] = {v.getX(), v.getY(), v.getZ()};
		int depthAxis = sliceAxis.index();
		for (int i = 0; i < 3; ++i) {
			if ( (i == depthAxis) && (indexStyle == TileIndex.IndexStyle.QUADTREE) )
				continue; // don't zoom on slice axis in quadtree mode
			xyz[i] /= zoomFactor;
		}
		return new ZoomedVoxelIndex(zoomLevel, xyz[0], xyz[1], xyz[2]);
	}
	
	/**
	 * ZoomedVoxel at upper left front corner of tile
	 * @param t
	 * @param sliceAxis
	 * @return
	 */
	public ZoomedVoxelIndex zoomedVoxelIndexForTileXyz(TileXyz t, ZoomLevel zoomLevel, CoordinateAxis sliceAxis) {
		int xyz[] = {t.getX(), t.getY(), t.getZ()};
		int depthAxis = sliceAxis.index();
		for (int i = 0; i < 3; ++i) {
			if (i == depthAxis)
				continue; // Don't scale depth axis
			else
				xyz[i] = xyz[i]*getTileSize()[i]; // scale horizontal and vertical
		}
		// Invert Y axis to convert to Raveler convention from image convention.
		int zoomFactor = zoomLevel.getZoomOutFactor();
		int maxZoomVoxelY = volumeSize[1] / zoomFactor;
		xyz[1] = maxZoomVoxelY - xyz[1] - getTileSize()[1];
		//
		return new ZoomedVoxelIndex(zoomLevel, xyz[0], xyz[1], xyz[2]);
	}
	/**
	 * TileIndex xyz containing ZoomedVoxel
	 * @param z
	 * @param zoomLevel
	 * @param sliceAxis
	 * @return
	 */
	public TileXyz tileXyzForZoomedVoxelIndex(ZoomedVoxelIndex z, CoordinateAxis sliceAxis) {
		int xyz[] = {z.getX(), z.getY(), z.getZ()};
		// Invert Y axis to convert to Raveler convention from image convention.
		int zoomFactor = z.getZoomLevel().getZoomOutFactor();
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
	public static class MicrometerXyz extends UnittedVec3Double<MicrometerUnit>
	{
		public MicrometerXyz(double x, double y, double z) {super(x,y,z);}
	}; // 1	
	
	public static class VoxelXyz extends UnittedVec3Int<VoxelUnit> 
	{
		public VoxelXyz(int x, int y, int z) {super(x, y, z);}
        public VoxelXyz(int[] xyz) {super(xyz[0], xyz[1], xyz[2]);}
	}; // 2
	
	/* OBSOLETED in favor of ...octree.ZoomedVoxelIndex
	public static class ZoomedVoxelIndex extends UnittedVec3Int<ZoomedVoxelUnit> 
	{		
		public ZoomedVoxelIndex(int x, int y, int z) {super(x, y, z);}
	}; // 3
	*/
	
	public static class TileXyz extends UnittedVec3Int<TileUnit> 
	{
		public TileXyz(int x, int y, int z) {super(x, y, z);}		
	} // 4

}
