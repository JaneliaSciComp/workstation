package org.janelia.it.workstation.gui.large_volume_viewer;

import static org.junit.Assert.*;


import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.jacs.model.TestCategories;
import org.janelia.it.workstation.octree.ZoomLevel;
import org.janelia.it.workstation.octree.ZoomedVoxelIndex;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TestTileFormat {

	// Volume like "M:/render/2013-04-25-AAV/"
	private TileFormat createAavFormat() {
		TileFormat tileFormat = new TileFormat();
		tileFormat.setDefaultParameters();
		tileFormat.setVolumeSize(new int[] {65536, 65536, 7936});
		tileFormat.setVoxelMicrometers(new double[] {1.0, 1.0, 1.0});
		tileFormat.setTileSize(new int[] {1024, 1024, 124});
		tileFormat.setZoomLevelCount(7);
		return tileFormat;
	}
	
	// xyz->TileIndex->cornerXyz sanity check
	private void sanityCheckXyz(Vec3 xyz, TileFormat format, int zoom) {
		TileIndex ix = format.tileIndexForXyz(xyz, zoom, CoordinateAxis.Z);
		Vec3 corners[] = cornersForTileIndex(ix, format);
		// Verify that the tile corners bound the seed point xyz
		double epsilon = 1e-6; // roundoff error tolerance
		assertTrue(xyz.getX() >= corners[0].getX() - epsilon);
		assertTrue(xyz.getX() <= corners[1].getX() + epsilon);
		// 
		assertTrue(xyz.getY() >= corners[0].getY() - epsilon);
		assertTrue(xyz.getY() <= corners[3].getY() + epsilon);
		// From Les Foster test 8/19/2013
		// Verify relative position of corners
		assertTrue(corners[0].getY() <= corners[3].getY());
		assertTrue(corners[0].getX() <= corners[3].getX());
		// Verify that adjacent tiles perfectly cover space
		// Consider another tile to the lower right of ix
		TileIndex ixDiag = new TileIndex(
				ix.getX()+1, ix.getY()-1, ix.getZ(),
				ix.getZoom(), ix.getMaxZoom(), 
				ix.getIndexStyle(),
				ix.getSliceAxis());
		Vec3 cornersDiag[] = cornersForTileIndex(ixDiag, format);
		assertEquals(corners[3].getX(), cornersDiag[0].getX(), epsilon);
		assertEquals(corners[3].getY(), cornersDiag[0].getY(), epsilon);
		assertEquals(corners[3].getZ(), cornersDiag[0].getZ(), epsilon);
	}
	
	@Test
    @Category(TestCategories.FastTests.class)
    public void testTileIndexForXyz() {
		TileFormat tileFormat = createAavFormat();
		
		// Test simple upper left front corner tile
		Vec3 xyz1 = new Vec3(0, 0, 0);
		TileIndex ix1 = tileFormat.tileIndexForXyz(xyz1, 0, CoordinateAxis.Z);
		assertEquals(0, ix1.getX());
		assertEquals(63, ix1.getY());
		assertEquals(0, ix1.getZ());

		// Test general tile
		Vec3 xyz2 = new Vec3(29952.0, 24869.6, 1243.5);
		TileIndex ix2 = tileFormat.tileIndexForXyz(xyz2, 0, CoordinateAxis.Z);
		assertEquals(29, ix2.getX());
		assertEquals(39, ix2.getY());
		assertEquals(1243, ix2.getZ());
		
		sanityCheckXyz(new Vec3(0,0,0), tileFormat, 0);
		sanityCheckXyz(new Vec3(2048,2048,0), tileFormat, 0);
		sanityCheckXyz(new Vec3(2047,2047,0), tileFormat, 0);
		sanityCheckXyz(new Vec3(2049,2049,0), tileFormat, 0);
		
		// Non-zero zoom
		sanityCheckXyz(new Vec3(0,0,0), tileFormat, 3);
		sanityCheckXyz(new Vec3(2048,2048,0), tileFormat, 3);
		sanityCheckXyz(new Vec3(2047,2047,0), tileFormat, 3);
		sanityCheckXyz(new Vec3(2049,2049,0), tileFormat, 3);
		
		// TODO - non-Z slices
	}

	@Test
    @Category(TestCategories.FastTests.class)
	public void testCornersForTileIndex() {
		TileFormat tileFormat = createAavFormat();

		// upper left front corner tile
		TileIndex ix1 = new TileIndex(0, 63, 0,
				0, 0, 
				TileIndex.IndexStyle.OCTREE,
				CoordinateAxis.Z);
		Vec3 corners[] = cornersForTileIndex(ix1, tileFormat);
		// Order of corners should be like this:
		//  0 --- 1
		//  |     |
		//  |     |
		//  2 --- 3
		//
		// X coordinate : the easy case
		double epsilon = 1e-6;
		assertEquals(0, corners[0].getX(), epsilon);
		assertEquals(0, corners[2].getX(), epsilon);
		assertEquals(1024.0, corners[1].getX(), epsilon);
		assertEquals(1024.0, corners[3].getX(), epsilon);
		// Y : trickier because of inversion between Raveler and image order
		assertEquals(0.0, corners[0].getY(), epsilon);
		assertEquals(0.0, corners[1].getY(), epsilon);
		assertEquals(1024.0, corners[2].getY(), epsilon);
		assertEquals(1024.0, corners[3].getY(), epsilon);
		// Z : constant
		assertEquals(0.5, corners[0].getZ(), epsilon);
		assertEquals(0.5, corners[1].getZ(), epsilon);
		assertEquals(0.5, corners[2].getZ(), epsilon);
		assertEquals(0.5, corners[3].getZ(), epsilon);
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
	public Vec3[] cornersForTileIndex(TileIndex index, TileFormat format) {
		// New way
		// upper left front corner of tile
	    ZoomLevel zoomLevel = new ZoomLevel(index.getZoom());
		TileFormat.TileXyz tileXyz = new TileFormat.TileXyz(index.getX(), index.getY(), index.getZ());
		ZoomedVoxelIndex zvox = format.zoomedVoxelIndexForTileXyz(tileXyz, zoomLevel, index.getSliceAxis());
		TileFormat.VoxelXyz vox = format.voxelXyzForZoomedVoxelIndex(zvox, index.getSliceAxis());
		TileFormat.MicrometerXyz ulfCorner = format.micrometerXyzForVoxelXyz(vox, index.getSliceAxis());
		// lower right back corner
		int dt[] = {1, -1, 1}; // shift by one tile to get opposite corner
		int depthAxis = index.getSliceAxis().index();
		dt[depthAxis] = 0; // but no shift in slice direction
		TileFormat.TileXyz tileLrb = new TileFormat.TileXyz(index.getX() + dt[0], index.getY() + dt[1], index.getZ() + dt[2]);
		ZoomedVoxelIndex zVoxLrb = format.zoomedVoxelIndexForTileXyz(tileLrb, zoomLevel, index.getSliceAxis());
		TileFormat.VoxelXyz voxLrb = format.voxelXyzForZoomedVoxelIndex(zVoxLrb, index.getSliceAxis());
		TileFormat.MicrometerXyz lrbCorner = format.micrometerXyzForVoxelXyz(voxLrb, index.getSliceAxis());

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
	
}
