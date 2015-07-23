/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Testing how point enclosures work.
 * 
 * @author fosterl
 */
public class PointEnclosureFactoryTest {
    
    public PointEnclosureFactoryTest() {
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
    public void enclosePoints() {
        VertexNumberGenerator vng = new VertexNumberGenerator();
        PointEnclosureFactory factory = new PointEnclosureFactory(10, 10.0, vng);
        factory.getVertices();        
    }

}
