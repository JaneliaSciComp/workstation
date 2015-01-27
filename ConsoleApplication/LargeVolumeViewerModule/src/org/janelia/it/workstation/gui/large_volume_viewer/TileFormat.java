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
    private static final int X_OFFS = 0;
    private static final int Y_OFFS = 1;
    private static final int Z_OFFS = 2;
    
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
		Vec3 b0 = new Vec3(calcLowerBBCoord(X_OFFS), calcLowerBBCoord(Y_OFFS), calcLowerBBCoord(Z_OFFS));
		Vec3 b1 = new Vec3(calcUpperBBCoord(X_OFFS), calcUpperBBCoord(Y_OFFS), calcUpperBBCoord(Z_OFFS));
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
		double maxRes = Math.min(vm[X_OFFS], Math.min(vm[Y_OFFS], vm[Z_OFFS]));
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
	
    public TileBoundingBox viewBoundsToTileBounds(int[] xyzFromWhd, ViewBoundingBox screenBounds, int zoom) {

        double zoomFactor = Math.pow(2.0, zoom);
		// get tile pixel size 1024 from loadAdapter
        double resolution0 = getVoxelMicrometers()[xyzFromWhd[0]];
        double resolution1 = getVoxelMicrometers()[xyzFromWhd[1]];
		double tileWidth = tileSize[xyzFromWhd[0]] * zoomFactor * resolution0;
		double tileHeight = tileSize[xyzFromWhd[1]] * zoomFactor * resolution1;

		int wMin = (int)Math.floor(screenBounds.getwFMin() / tileWidth);
		int wMax = (int)Math.floor(screenBounds.getwFMax() / tileWidth);

		int hMin = (int)Math.floor(screenBounds.gethFMin() / tileHeight);
		int hMax = (int)Math.floor(screenBounds.gethFMax() / tileHeight);
        
        TileBoundingBox tileUnits = new TileBoundingBox();
        tileUnits.sethMax(hMax);
        tileUnits.sethMin(hMin);
        tileUnits.setwMax(wMax);
        tileUnits.setwMin(wMin);
        
        return tileUnits;
    }
    
    /**
     * This will use the meaningful intersection of the bounding box and
     * the view, and take 1/2-unit-over-boundary into consideration.  It will
     * produce a set of width and height bounds that contain the focus
     * point, within the stated width and height.
     * Output includes view-width and view-height sized rectangle, with focus as
     * near to center as possible
     * 
     * @param viewWidth how wide to make surrounding view in wide axis.
     * @param viewHeight how wide to make surrounding view in high axis.
     * @param focus output must include (ideally, centered) this point.
     * @param pixelsPerViewUnit used for calculating surrounding rectangle.
     * @param xyzFromWhd indirection for axial sequence numbers.
     * @return min/max-delimiting box.
     */
    public ViewBoundingBox findViewBounds(
            int viewWidth, int viewHeight, Vec3 focus, double pixelsPerViewUnit, int[] xyzFromWhd 
    ) {
        BoundingBox3d bb = calcBoundingBox();
        bb = originAdjustBoundingBox(bb, xyzFromWhd);
        focus = originAdjustCameraFocus(focus, xyzFromWhd);
        
		// Clip to bounded space  
        double xMinView = focus.get(xyzFromWhd[X_OFFS]) - 0.5*viewWidth/pixelsPerViewUnit;
        double xMaxView = focus.get(xyzFromWhd[X_OFFS]) + 0.5*viewWidth/pixelsPerViewUnit;
        
		double yMinView = focus.get(xyzFromWhd[Y_OFFS]) - 0.5*viewHeight/pixelsPerViewUnit;
		double yMaxView = focus.get(xyzFromWhd[Y_OFFS]) + 0.5*viewHeight/pixelsPerViewUnit;

		// Subtract one half pixel to avoid loading an extra layer of tiles
		double dw = 0.25 * getVoxelMicrometers()[xyzFromWhd[X_OFFS]];
		double dh = 0.25 * getVoxelMicrometers()[xyzFromWhd[Y_OFFS]];

        // Invert Y for target coordinate system.
		double bottomY = bb.getMax().getY();
        
        double xMinViewUnit = Math.max(xMinView, bb.getMin().get(xyzFromWhd[X_OFFS]) + dw);
		double yMinViewUnit = bottomY - Math.max(yMinView, bb.getMin().get(xyzFromWhd[Y_OFFS]) + dh);
        
		double xMaxViewUnit = Math.min(xMaxView, bb.getMax().get(xyzFromWhd[X_OFFS]) - dw);
		double yMaxViewUnit = bottomY - Math.min(yMaxView, bb.getMax().get(xyzFromWhd[Y_OFFS]) - dh);
        
        // The y-flip correction is wrong when the origin is not at zero. 
        // I think I got that corrected by adding the minimum Y value during the flip:
        //   Editor correction: bottomY is same thing.
		// Correct for bottom Y origin of Raveler tile coordinate system
		// (everything else is top Y origin: image, our OpenGL, user facing coordinate system)
		if (xyzFromWhd[X_OFFS] == Y_OFFS) { // Y axis left-right
			double temp = xMinViewUnit;
			xMinViewUnit = xMaxViewUnit;
			xMaxViewUnit = temp;
		}
		else if (xyzFromWhd[Y_OFFS] == Y_OFFS) { // Y axis top-bottom
			double temp = yMinViewUnit;
			yMinViewUnit = yMaxViewUnit;
			yMaxViewUnit = temp;
		}
		else {
			// TODO - invert slice axis? (already inverted above)
		}
        
        ViewBoundingBox viewBoundaries = new ViewBoundingBox();
        viewBoundaries.sethFMax(yMaxViewUnit);
        viewBoundaries.sethFMin(yMinViewUnit);
        
        viewBoundaries.setwFMax(xMaxViewUnit);
        viewBoundaries.setwFMin(xMinViewUnit);
        return viewBoundaries;
    }
    
    public int calcRelativeTileDepth(int[] xyzFromWhd, double focusDepth, BoundingBox3d bb) {
        // Bounding box is actually 0.5 voxels bigger than number of slices at each end
        int dMin = (int)(bb.getMin().get(xyzFromWhd[Z_OFFS])/getVoxelMicrometers()[xyzFromWhd[Z_OFFS]] + 0.5);
        int dMax = (int)(bb.getMax().get(xyzFromWhd[Z_OFFS])/getVoxelMicrometers()[xyzFromWhd[Z_OFFS]] - 0.5);
        int absoluteTileDepth = (int)Math.round(focusDepth / getVoxelMicrometers()[xyzFromWhd[Z_OFFS]] - 0.5);
        absoluteTileDepth = Math.max(absoluteTileDepth, dMin);
        absoluteTileDepth = Math.min(absoluteTileDepth, dMax);
        int relativeTileDepth = absoluteTileDepth - getOrigin()[xyzFromWhd[Z_OFFS]];
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
		tileSize[Z_OFFS] = 1; // tiles are 512x512x1
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
		int w = getTileSize()[X_OFFS];
		int h = getTileSize()[Y_OFFS];
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
				v.getX() + origin[X_OFFS],
				v.getY() + origin[Y_OFFS],
				v.getZ() + origin[Z_OFFS],
		};
		xyz[sliceDirection.index()] += 0.5;
		return new MicrometerXyz(
				xyz[0] * getVoxelMicrometers()[X_OFFS],
				xyz[1]  * getVoxelMicrometers()[Y_OFFS],				
				xyz[2]  * getVoxelMicrometers()[Z_OFFS]);
	}
	public VoxelXyz voxelXyzForMicrometerXyz(MicrometerXyz m) {
		return new VoxelXyz(
				(int)Math.floor(m.getX() / getVoxelMicrometers()[X_OFFS]) - origin[X_OFFS],
				(int)Math.floor(m.getY() / getVoxelMicrometers()[Y_OFFS]) - origin[Y_OFFS],
				(int)Math.floor(m.getZ() / getVoxelMicrometers()[Z_OFFS]) - origin[Z_OFFS]);
	}

    /** 
     * convenience: return a centered-up version of the micrometer value.
     * Use this whenever micrometer values need to be pushed onto the screen.
     */
    public Vec3 centerJustifyMicrometerCoordsAsVec3(MicrometerXyz microns) {
        Vec3 v = new Vec3(
                // Translate from upper left front corner of voxel to center of voxel
                microns.getX() + 0.5 * voxelMicrometers[0],
                microns.getY() + 0.5 * voxelMicrometers[1],
                microns.getZ() - 0.5 * voxelMicrometers[2]);
        return v;
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
		return new VoxelXyz(xyz[X_OFFS], xyz[Y_OFFS], xyz[Z_OFFS]);
	}
    
    public TileFormat.MicrometerXyz micrometerXyzForZoomedVoxelIndex( ZoomedVoxelIndex zv, CoordinateAxis axis ) {
        TileFormat.VoxelXyz vx = voxelXyzForZoomedVoxelIndex(zv, axis);
        return micrometerXyzForVoxelXyz(vx, axis);
    }
    
    /** Convenience method for conversion. */
    public Vec3 micronVec3ForVoxelVec3( Vec3 voxelVec3 ) {
        TileFormat.VoxelXyz vox = new TileFormat.VoxelXyz(voxelVec3);
        TileFormat.MicrometerXyz micron = micrometerXyzForVoxelXyz(vox, CoordinateAxis.Z);
        return new Vec3( micron.getX(), micron.getY(), micron.getZ() );
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
		return new ZoomedVoxelIndex(zoomLevel, xyz[X_OFFS], xyz[Y_OFFS], xyz[Z_OFFS]);
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
		int maxZoomVoxelY = volumeSize[Y_OFFS] / zoomFactor;
		xyz[1] = maxZoomVoxelY - xyz[Y_OFFS] - getTileSize()[Y_OFFS];
		//
		return new ZoomedVoxelIndex(zoomLevel, xyz[X_OFFS], xyz[Y_OFFS], xyz[Z_OFFS]);
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
		int maxZoomVoxelY = volumeSize[Y_OFFS] / zoomFactor - 1;
		xyz[1] = maxZoomVoxelY - xyz[1];
		int depthAxis = sliceAxis.index();
		for (int i = 0; i < 3; ++i) {
			if (i == depthAxis)
				continue; // Don't scale depth axis
			else
				xyz[i] = xyz[i]/getTileSize()[i]; // scale horizontal and vertical
		}
		return new TileXyz(xyz[X_OFFS], xyz[Y_OFFS], xyz[Z_OFFS]);
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
			data[X_OFFS] = x;
			data[Y_OFFS] = y;
			data[Z_OFFS] = z;
		}
		
		@Override
		public UnittedVec3Int<U> clone() {
			return new UnittedVec3Int<U>(data[X_OFFS], data[1], data[2]);
		}
		
		public int getX() {return data[X_OFFS];}
		public int getY() {return data[Y_OFFS];}
		public int getZ() {return data[Z_OFFS];}
	}; 
	
	public static class UnittedVec3Double<U extends Unit> 
	implements Cloneable
	{
		private double data[] = new double[3];
		
		public UnittedVec3Double(double x, double y, double z) {
			data[X_OFFS] = x;
			data[Y_OFFS] = y;
			data[Z_OFFS] = z;
		}
		
		@Override
		public UnittedVec3Double<U> clone() {
			return new UnittedVec3Double<U>(data[X_OFFS], data[Y_OFFS], data[Z_OFFS]);
		}
		
		public double getX() {return data[X_OFFS];}
		public double getY() {return data[Y_OFFS];}
		public double getZ() {return data[Z_OFFS];}
	}; 
	
	// Base
	public static class MicrometerXyz extends UnittedVec3Double<MicrometerUnit>
	{
		public MicrometerXyz(double x, double y, double z) {super(x,y,z);}
	}; // 1	
	
	public static class VoxelXyz extends UnittedVec3Int<VoxelUnit> 
	{
		public VoxelXyz(Vec3 coords) {super((int)coords.getX(), (int)coords.getY(), (int)coords.getZ());}
		public VoxelXyz(int x, int y, int z) {super(x, y, z);}
        public VoxelXyz(int[] xyz) {super(xyz[X_OFFS], xyz[Y_OFFS], xyz[Z_OFFS]);}
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

    @SuppressWarnings("unused")
    private void bbToScreenScenarioDump(ViewBoundingBox screenBoundaries, BoundingBox3d bb, int viewWidth, int viewHeight) {
        System.out.println("================================================");
        System.out.println("SCENARIO: TileFormat.boundingBoxToScreenBounds()");
        System.out.println("View width=" + viewWidth + ", View Height=" + viewHeight);
        System.out.println( screenBoundaries );
        System.out.println( bb.toString() );
    }
    
    /**
     * This method will remove origin offset from the bounding box, to make
     * it more convenient.
     * @param bb some origin-based bounding box.
     * @return offset to the 0 of the screen (as if ori=0,0,0).
     */
    private BoundingBox3d originAdjustBoundingBox( BoundingBox3d bb, int[] xyzFromWhd ) {
        BoundingBox3d rtnVal = new BoundingBox3d();
        final Vec3 originVec = new Vec3(
                origin[xyzFromWhd[0]] * voxelMicrometers[xyzFromWhd[0]],
                0,
                0
        );
        Vec3 min = bb.getMin().minus(originVec);
        Vec3 max = bb.getMax().minus(originVec);
        rtnVal.setMin(min);
        rtnVal.setMax(max);
        return rtnVal;
    }
    
    private Vec3 originAdjustCameraFocus( Vec3 focus, int[] xyzFromWhd ) {
        Vec3 rtnVal = new Vec3(
            focus.get(xyzFromWhd[X_OFFS]) - origin[xyzFromWhd[0]]*voxelMicrometers[xyzFromWhd[X_OFFS]],
            focus.get(xyzFromWhd[Y_OFFS]),
            focus.get(xyzFromWhd[Z_OFFS])
        );
            
        return rtnVal;
    }

    private double calcLowerBBCoord(int index) {
        //sv[1]*s0[1]
        return getVoxelMicrometers()[index] * getOrigin()[index];
    }
    
    private double calcUpperBBCoord(int index) {
        //double sv[] = getVoxelMicrometers();
		//int s0[] = getOrigin();
		//int s1[] = getVolumeSize();
        //sv[1]*(s0[1]+s1[1])
        return getVoxelMicrometers()[index] * (getOrigin()[index] + getVolumeSize()[index]);
    }
    
}
