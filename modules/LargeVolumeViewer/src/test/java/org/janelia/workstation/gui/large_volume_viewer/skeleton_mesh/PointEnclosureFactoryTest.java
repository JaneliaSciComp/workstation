package org.janelia.workstation.gui.large_volume_viewer.skeleton_mesh;

import org.janelia.workstation.gui.large_volume_viewer.skeleton_mesh.PointEnclosureFactory;
import org.janelia.workstation.gui.large_volume_viewer.skeleton_mesh.VertexNumberGenerator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
