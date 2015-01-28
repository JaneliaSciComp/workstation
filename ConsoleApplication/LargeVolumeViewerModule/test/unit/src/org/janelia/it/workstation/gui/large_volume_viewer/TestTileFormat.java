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
		sanityTest(tileFormat);		
	}

	@Test
    @Category(TestCategories.FastTests.class)
    public void testTileIndexForXyzAsStage() {
		TileFormat tileFormat = createAavFormat();
        // Add sufficient information to make these coords stage coords.
        // Using convenient numbers for the offsets and pixel sizes.
        tileFormat.setOrigin(new int[] {
            200000, 140000, 19000
        });
        tileFormat.setVoxelMicrometers(
            new double[] {
                0.5, 0.25, 0.1
            }
        );
        Vec3[] vecComparison = new Vec3[] {
            new Vec3(160,  -72,  0.05),
            new Vec3(2208, 1976, 0.05),
            new Vec3(2208, 2232, 0.05),
            new Vec3(2720, 2232, 0.05)
        };
        final int[] ixComparison1 = new int[]{ -195, 200, -19000 };
        final int[] ixComparison2 = new int[] {-136, 103, -6565};

		sanityTest(tileFormat, ixComparison1, ixComparison2, vecComparison);		
	}

	@Test
    @Category(TestCategories.FastTests.class)
	public void testCornersForTileIndex() {
		TileFormat tileFormat = createAavFormat();
		testCorners(tileFormat);
	}

    @Test
    @Category(TestCategories.FastTests.class)
    public void testMatrixConvVoxelForMicron() {
		TileFormat tileFormat = createAavFormat();
        TileFormat.MicrometerXyz startingValue = new TileFormat.MicrometerXyz( 2000, 1500, 500 );
        TileFormat.VoxelXyz firstConv = tileFormat.voxelXyzForMicrometerXyz(startingValue);
        TileFormat.VoxelXyz secondConv = tileFormat.voxelXyzForMicrometerXyzMatrix(startingValue);
        
        assertEquals("X not equal", firstConv.getX(), secondConv.getX());
        assertEquals("Y not equal", firstConv.getY(), secondConv.getY());
        assertEquals("Z not equal", firstConv.getZ(), secondConv.getZ());
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
	
    private void sanityTest(TileFormat tileFormat) {
        // Test simple upper left front corner tile
        Vec3[] vecComparison = new Vec3[] {
            new Vec3(0,0,0),
            new Vec3(2048,2048,0),
            new Vec3(2047,2047,0),
            new Vec3(2049,2049,0)
        };
        sanityTest(tileFormat, new int[]{ 0, 63, 0}, new int[] {29, 39, 1243}, vecComparison);
        // TODO - non-Z slices
    }

    private void sanityTest(TileFormat tileFormat, int[] ixComparison1, int[] ixComparison2, Vec3[] cornerComparison) {
        // Test simple upper left front corner tile
        Vec3 xyz1 = new Vec3(0, 0, 0);
        TileIndex ix1 = tileFormat.tileIndexForXyz(xyz1, 0, CoordinateAxis.Z);
        assertEquals(ixComparison1[0], ix1.getX());
        assertEquals(ixComparison1[1], ix1.getY());
        assertEquals(ixComparison1[2], ix1.getZ());
        
        // Test general tile
        Vec3 xyz2 = new Vec3(29952.0, 24869.6, 1243.5);
        TileIndex ix2 = tileFormat.tileIndexForXyz(xyz2, 0, CoordinateAxis.Z);
        assertEquals(ixComparison2[0], ix2.getX());
        assertEquals(ixComparison2[1], ix2.getY());
        assertEquals(ixComparison2[2], ix2.getZ());
        
        sanityCheckXyz(cornerComparison[0], tileFormat, 0);
        sanityCheckXyz(cornerComparison[1], tileFormat, 0);
        sanityCheckXyz(cornerComparison[2], tileFormat, 0);
        sanityCheckXyz(cornerComparison[3], tileFormat, 0);
        
        // Non-zero zoom
        sanityCheckXyz(cornerComparison[0], tileFormat, 3);
        sanityCheckXyz(cornerComparison[1], tileFormat, 3);
        sanityCheckXyz(cornerComparison[2], tileFormat, 3);
        sanityCheckXyz(cornerComparison[3], tileFormat, 3);
        
        // TODO - non-Z slices
    }

    private void testCorners(TileFormat tileFormat) {
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

}
