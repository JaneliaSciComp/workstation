/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh;

import org.janelia.it.jacs.shared.mesh_loader.VertexInfoBean;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author fosterl
 */
public class LineEnclosureFactoryTest {
    private Logger logger = LoggerFactory.getLogger(LineEnclosureFactoryTest.class);
    public LineEnclosureFactoryTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void hexagonalPrismEnclosure() {
        LineEnclosureFactory factory = new LineEnclosureFactory( 6, 16.0 );
        
        int totalCoordCount = 0;
		double[] origin = {0,0,0};
		double[] coords = new double[] {1,0,1};
        totalCoordCount = testCoordPair(totalCoordCount, origin, coords, factory);
        logger.info("1. Coord count " + formatCoords(coords) + totalCoordCount);
        
        //2
		coords = new double[] {-1,0,1};
        totalCoordCount = testCoordPair(totalCoordCount, origin, coords, factory);
        logger.info("2. Coord count " + formatCoords(coords) + totalCoordCount);

        //3
		coords = new double[] {1,0,-1};
        totalCoordCount = testCoordPair(totalCoordCount, origin, coords, factory);
        logger.info("3. Coord count (" + formatCoords(coords) + totalCoordCount);
        
        //4
		coords =  new double[] {1,1,1};
        totalCoordCount = testCoordPair(totalCoordCount, origin, coords, factory);
        logger.info("4. Coord count " + formatCoords(coords) + totalCoordCount);
        
        //5
		coords = new double[] {1,1,0};
        totalCoordCount = testCoordPair(totalCoordCount, origin, coords, factory);
        logger.info("5. Coord count " + formatCoords(coords) + totalCoordCount);
        
        //6
		coords = new double[] {-1,1,0};
        totalCoordCount = testCoordPair(totalCoordCount, origin, coords, factory);
        logger.info("6. Coord count " + formatCoords(coords) + totalCoordCount);
        
        //7
		coords = new double[] {1,-1,0};
        totalCoordCount = testCoordPair(totalCoordCount, origin, coords, factory);
        logger.info("7. Coord count " + formatCoords(coords) + totalCoordCount);
        
        //8
		coords = new double[] {0,1,1};
        totalCoordCount = testCoordPair(totalCoordCount, origin, coords, factory);
        logger.info("8. Coord count " + formatCoords(coords) + totalCoordCount);
        
        //9
		coords = new double[] {0,-1,1};
        totalCoordCount = testCoordPair(totalCoordCount, origin, coords, factory);
        logger.info("9. Coord count " + formatCoords(coords) + totalCoordCount);
        
        //10
		coords = new double[] {0,1,0};
        totalCoordCount = testCoordPair(totalCoordCount, origin, coords, factory);
        logger.info("10. Coord count " + formatCoords(coords) + totalCoordCount);
        
		//11
		coords = new double[] {-1.732, 0, 1};
        totalCoordCount = testCoordPair(totalCoordCount, origin, coords, factory);
        logger.info("11. Coord count " + formatCoords(coords) + totalCoordCount);
        
		//12
		coords = new double[] {1.732, 0, -1};
        totalCoordCount = testCoordPair(totalCoordCount, origin, coords, factory);
        logger.info("12. Coord count " + formatCoords(coords) + totalCoordCount);
        
		//13
		coords = new double[] {0,1,1.732};
        totalCoordCount = testCoordPair(totalCoordCount, origin, coords, factory);
        logger.info("13. Coord count " + formatCoords(coords) + totalCoordCount);

		//14
		coords = new double[] {0,-1,1.732};
        totalCoordCount = testCoordPair(totalCoordCount, origin, coords, factory);
        logger.info("14. Coord count " + formatCoords(coords) + totalCoordCount);

		//15
		coords = new double[] {0,1,-1.732};
        totalCoordCount = testCoordPair(totalCoordCount, origin, coords, factory);
        logger.info("15. Coord count " + formatCoords(coords) + totalCoordCount);

		//16
		coords = new double[] {0,-1,-1.732};
        totalCoordCount = testCoordPair(totalCoordCount, origin, coords, factory);
        logger.info("16. Coord count " + formatCoords(coords) + totalCoordCount);

        //17
		coords = new double[] {0,1,1.732};
        totalCoordCount = testCoordPair(totalCoordCount, origin, coords, factory);
        logger.info("17. Coord count " + formatCoords(coords) + totalCoordCount);
        
        //18
		coords = new double[] {0,-1,1.732};
        totalCoordCount = testCoordPair(totalCoordCount, origin, coords, factory);
        logger.info("18. Coord count " + formatCoords(coords) + totalCoordCount);
        
        //19
		coords = new double[] {0,1,-1.732};
        totalCoordCount = testCoordPair(totalCoordCount, origin, coords, factory);
        logger.info("19. Coord count " + formatCoords(coords) + totalCoordCount);
        
        //20
		coords = new double[] {-1.732,-1, 0};
        totalCoordCount = testCoordPair(totalCoordCount, origin, coords, factory);
        logger.info("20. Coord count " + formatCoords(coords) + totalCoordCount);
        
		int vtxNum = 1;
        int absVtxCount = 0;
        for (VertexInfoBean vtx: factory.getVertices()) {
            int inVertexCoordNum = 0;
            for (float coord: vtx.getCoordinates()) {
                assertTrue("Coord is not a number.  Absolute coord number: " + absVtxCount + ", in-vertex coordinate " + inVertexCoordNum + " in vertex number " + vtxNum,
                           ! Double.isNaN(coord));
                absVtxCount++;
                inVertexCoordNum ++;
            }
            vtxNum ++;
        }
    }

	private int testCoordPair(int totalCoordCount, double[] startingCoords, double[] endingCoords, LineEnclosureFactory factory) {
		totalCoordCount += factory.addEnclosure(startingCoords, endingCoords);
		return totalCoordCount;
	}
	
	private String formatCoords(double[] coords) {
		return String.format("(%f, %f, %f) ", coords[0], coords[1], coords[2]);
	}
}
