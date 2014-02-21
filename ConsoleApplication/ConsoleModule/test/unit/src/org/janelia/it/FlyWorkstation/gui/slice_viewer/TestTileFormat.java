package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import static org.junit.Assert.*;


import org.janelia.it.FlyWorkstation.geom.CoordinateAxis;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.junit.Test;

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
		Vec3 corners[] = format.cornersForTileIndex(ix);
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
		Vec3 cornersDiag[] = format.cornersForTileIndex(ixDiag);
		assertEquals(corners[3].getX(), cornersDiag[0].getX(), epsilon);
		assertEquals(corners[3].getY(), cornersDiag[0].getY(), epsilon);
		assertEquals(corners[3].getZ(), cornersDiag[0].getZ(), epsilon);
	}
	
	@Test
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
	public void testCornersForTileIndex() {
		TileFormat tileFormat = createAavFormat();

		// upper left front corner tile
		TileIndex ix1 = new TileIndex(0, 63, 0,
				0, 0, 
				TileIndex.IndexStyle.OCTREE,
				CoordinateAxis.Z);
		Vec3 corners[] = tileFormat.cornersForTileIndex(ix1);
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

	@Test
	public void testZoomLevelForCameraZoom() {
		// fail("Not yet implemented");
	}

}
