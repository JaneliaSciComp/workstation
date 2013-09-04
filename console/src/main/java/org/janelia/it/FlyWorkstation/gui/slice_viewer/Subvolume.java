package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;
import java.util.Set;

import org.janelia.it.FlyWorkstation.geom.CoordinateAxis;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.AbstractTextureLoadAdapter.MissingTileException;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.AbstractTextureLoadAdapter.TileLoadError;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.signal.Signal1;

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
		int minTileXyz[] =  {
				Math.min(cornerTileIx1.getX(), cornerTileIx2.getX()),
				Math.min(cornerTileIx1.getY(), cornerTileIx2.getY()),
				Math.min(cornerTileIx1.getZ(), cornerTileIx2.getZ())};
		int maxTileXyz[] =  {
				Math.max(cornerTileIx1.getX(), cornerTileIx2.getX()),
				Math.max(cornerTileIx1.getY(), cornerTileIx2.getY()),
				Math.max(cornerTileIx1.getZ(), cornerTileIx2.getZ())};
		// Check for case where entire subvolume is outside of parent Volume
		boolean isEmpty = false;
		BoundingBox3d bb = tileFormat.calcBoundingBox();
		TileIndex minGlobalVolTileIx = tileFormat.tileIndexForXyz(bb.getMin(), zoom, sliceDirection);
		TileIndex maxGlobalVolTileIx = tileFormat.tileIndexForXyz(bb.getMax(), zoom, sliceDirection);
		// filter by min/max, in case bounding box convention differs from TileIndex convention in Y
		int minGlobalTileXyz[] = {
				Math.min(minGlobalVolTileIx.getX(), maxGlobalVolTileIx.getX()),
				Math.min(minGlobalVolTileIx.getY(), maxGlobalVolTileIx.getY()),
				Math.min(minGlobalVolTileIx.getZ(), maxGlobalVolTileIx.getZ()),
		};
		int maxGlobalTileXyz[] = {
				Math.max(minGlobalVolTileIx.getX(), maxGlobalVolTileIx.getX()),
				Math.max(minGlobalVolTileIx.getY(), maxGlobalVolTileIx.getY()),
				Math.max(minGlobalVolTileIx.getZ(), maxGlobalVolTileIx.getZ()),
		};
		for (int i = 0; i < 3; ++i) {
			if (maxTileXyz[i] < minGlobalTileXyz[i])
				isEmpty = true; // entire subvolume below/left/front of parent
			if (minTileXyz[i] > maxGlobalTileXyz[i])
				isEmpty = true; // entire subvolume above/right/behind parent
		}
		if (isEmpty)
			return null;
		// Enumerate all tiles needed to fill this subvolume
		Set<TileIndex> neededTiles = new LinkedHashSet<TileIndex>();
		int maxZoom = cornerTileIx1.getMaxZoom();
		for (int x = minTileXyz[0]; x <= maxTileXyz[0]; ++x) {
			for (int y = minTileXyz[1]; y <= maxTileXyz[1]; ++y) {
				// Step through Z fastest for efficiency
				for (int z = minTileXyz[2]; z <= maxTileXyz[2]; ++z) {
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
