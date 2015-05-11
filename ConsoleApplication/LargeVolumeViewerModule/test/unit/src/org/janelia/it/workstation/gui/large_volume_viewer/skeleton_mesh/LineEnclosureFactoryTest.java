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
        double[] startingCoords;
        double[] endingCoords;
        
        int totalCoordCount = 0;
        //1
        startingCoords = new double[] {
            0,0,0
        };
        endingCoords = new double[] {
            1,0,1
        };
        totalCoordCount += factory.addEnclosure(startingCoords, endingCoords);
        logger.info("1. Coord count " + totalCoordCount);
        
        //2
        startingCoords = new double[] {
            0, 0, 0
        };
        endingCoords = new double[] {
            -1, 0, 1
        };
        totalCoordCount += factory.addEnclosure(startingCoords, endingCoords);
        logger.info("2. Coord count " + totalCoordCount);

        //3
        startingCoords = new double[] {
            2000.0, 2000.0, 1500.0
        };
        endingCoords = new double[] {
            2000.0, 2000.0, 2000.0
        };
        totalCoordCount += factory.addEnclosure(startingCoords, endingCoords);
        logger.info("3. Coord count " + totalCoordCount);
        
        //4
        startingCoords = new double[] {
            200.0, 200.0, 200.0
        };
        endingCoords = new double[]{
            100.0, 200.0, 200.0
        };
        totalCoordCount += factory.addEnclosure(startingCoords, endingCoords);
        logger.info("4. Coord count " + totalCoordCount);
        
        //5
        startingCoords = new double[]{
            0,0,0
        };
        endingCoords = new double[]{
            1,0,-1
        };
        totalCoordCount += factory.addEnclosure(startingCoords, endingCoords);
        logger.info("5. (45 degrees behind posi X) Coord count " + totalCoordCount);

        //6
        startingCoords = new double[]{
            0, 0, 0
        };
        endingCoords = new double[]{
            1, 1, 0
        };
        totalCoordCount += factory.addEnclosure(startingCoords, endingCoords);
        logger.info("6. (45 degrees below posi X) Coord count " + totalCoordCount);

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
}
