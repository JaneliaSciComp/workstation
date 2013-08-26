package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;
import java.util.Set;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.AbstractTextureLoadAdapter.MissingTileException;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.AbstractTextureLoadAdapter.TileLoadError;

public class Subvolume {
	
	private int extentVoxels[] = {0, 0, 0};
	
	// Load an octree subvolume into memory as a dense volume block
	public static Subvolume loadSubvolumeMicrometers(
			Vec3 corner1,
			Vec3 corner2,
			double micrometerResolution,
			SharedVolumeImage wholeImage) 
	{
		// Use the TileFormat class to convert between micrometer coordinates
		// and the arcane integer TileIndex coordinates.
		TileFormat tileFormat = wholeImage.getLoadAdapter().getTileFormat();
		// Compute correct zoom level based on requested resolution
		int zoom = tileFormat.zoomLevelForCameraZoom(1.0/micrometerResolution);
		// Compute extreme tile indices
		CoordinateAxis sliceDirection = CoordinateAxis.Z; // Z works best for most data sets
		TileIndex cornerTileIx1 = tileFormat.tileIndexForXyz(corner1, zoom, sliceDirection);
		TileIndex cornerTileIx2 = tileFormat.tileIndexForXyz(corner2, zoom, sliceDirection);
		// Sort coordinates into lower/left/front vs. upper/right/rear (Raveler-style convention in TileIndex)
		int xyzMin[] =  {
				Math.min(cornerTileIx1.getX(), cornerTileIx2.getX()),
				Math.min(cornerTileIx1.getY(), cornerTileIx2.getY()),
				Math.min(cornerTileIx1.getZ(), cornerTileIx2.getZ())};
		int xyzMax[] =  {
				Math.max(cornerTileIx1.getX(), cornerTileIx2.getX()),
				Math.max(cornerTileIx1.getY(), cornerTileIx2.getY()),
				Math.max(cornerTileIx1.getZ(), cornerTileIx2.getZ())};
		// Check for case where entire subvolume is outside of parent Volume
		boolean isEmpty = false;
		BoundingBox3d bb = tileFormat.calcBoundingBox();
		TileIndex wholeMinIx = tileFormat.tileIndexForXyz(bb.getMin(), zoom, sliceDirection);
		TileIndex wholeMaxIx = tileFormat.tileIndexForXyz(bb.getMax(), zoom, sliceDirection);
		// filter by min/max, in case bounding box convention differs from TileIndex convention in Y
		int wholeXyzMin[] = {
				Math.min(wholeMinIx.getX(), wholeMaxIx.getX()),
				Math.min(wholeMinIx.getY(), wholeMaxIx.getY()),
				Math.min(wholeMinIx.getZ(), wholeMaxIx.getZ()),
		};
		int wholeXyzMax[] = {
				Math.max(wholeMinIx.getX(), wholeMaxIx.getX()),
				Math.max(wholeMinIx.getY(), wholeMaxIx.getY()),
				Math.max(wholeMinIx.getZ(), wholeMaxIx.getZ()),
		};
		for (int i = 0; i < 3; ++i) {
			if (xyzMax[i] < wholeXyzMin[i])
				isEmpty = true; // entire subvolume below/left/front of parent
			if (xyzMin[i] > wholeXyzMax[i])
				isEmpty = true; // entire subvolume above/right/behind parent
		}
		if (isEmpty)
			return null;
		// Enumerate all tiles needed to fill this subvolume
		Set<TileIndex> neededTiles = new LinkedHashSet<TileIndex>();
		int maxZoom = cornerTileIx1.getMaxZoom();
		for (int x = xyzMin[0]; x <= xyzMax[0]; ++x) {
			for (int y = xyzMin[1]; y <= xyzMax[1]; ++y) {
				// Step through Z fastest for efficiency
				for (int z = xyzMin[2]; z <= xyzMax[2]; ++z) {
					TileIndex ix = new TileIndex(x, y, z, zoom,
							maxZoom, tileFormat.getIndexStyle(),
							sliceDirection);
					neededTiles.add(ix);
				}
			}
		}
		/// Load tile images ///
		// Compute micrometer subvolume corners in image convention
		Vec3 minCornerUm = new Vec3(
				Math.min(corner1.getX(), corner2.getX()),
				Math.min(corner1.getY(), corner2.getY()),
				Math.min(corner1.getZ(), corner2.getZ()));
		Vec3 maxCornerUm = new Vec3(
				Math.max(corner1.getX(), corner2.getX()),
				Math.max(corner1.getY(), corner2.getY()),
				Math.max(corner1.getZ(), corner2.getZ()));
		// TODO - use TextureCache, in case many of these image may have been loaded already
		AbstractTextureLoadAdapter loadAdapter = wholeImage.getLoadAdapter();
		for (TileIndex ix : neededTiles) {
			try {
				TextureData2dGL tileData = loadAdapter.loadToRam(ix);
				// TODO fill memory block
			} catch (TileLoadError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MissingTileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Compute voxel position in subvolume
			// Back to image convention (Y at top) [vs. Raveler convention, Y at bottom]
			Vec3 corners[] = tileFormat.cornersForTileIndex(ix);
			
			// TODO fill memory block
		}
		
		// TODO
		return new Subvolume();
	}
	
	public Signal1<Subvolume> subvolumeLoaded = new Signal1<Subvolume>();

	private Subvolume() {}

	public BufferedImage[] getAsBufferedImages() {
		int sx = extentVoxels[0];
		int sy = extentVoxels[1];
		int sz = extentVoxels[2];
		BufferedImage result[] = new BufferedImage[sz];
		for (int z = 0; z < sz; ++z) {
			// TODO
			/* result[z] = new BufferedImage(sx, sy,
					bufferedSlice1.getType());
					*/
		}
		return null;
	}
	
}
