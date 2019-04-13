package org.janelia.workstation.gui.large_volume_viewer.skeleton_mesh;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.janelia.it.jacs.shared.mesh_loader.RenderBuffersBean;
import org.janelia.it.jacs.shared.mesh_loader.TriangleSource;
import org.janelia.workstation.gui.large_volume_viewer.skeleton_mesh.LineEnclosureFactory;
import org.janelia.workstation.gui.large_volume_viewer.skeleton_mesh.NeuronTraceVtxAttribMgr;
import org.janelia.workstation.gui.large_volume_viewer.skeleton_mesh.VertexNumberGenerator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author fosterl
 */
public class NeuronTraceVtxAttribMgrTest {
    
    public NeuronTraceVtxAttribMgrTest() {
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
    public void buildAttributes() throws Exception {
        VertexNumberGenerator vng = new VertexNumberGenerator();
        LineEnclosureFactory factory = new LineEnclosureFactory(6, 16.0, vng);
        double[] startingCoords = new double[] {
            300, 210, 520
        };
        double[] endingCoords = new double[] {
            280, 200, 500
        }; 
        List<TriangleSource> triangleSources = new ArrayList<>();
        Map<Long, RenderBuffersBean> renderIdToBuffers = new HashMap<>(); 
        int coordCount = factory.addEnclosure(startingCoords, endingCoords);
        startingCoords = new double[] {
            0,0,0
        };
        endingCoords = new double[] {
            10,15,20  
        };
        coordCount += factory.addEnclosure(startingCoords, endingCoords);
        triangleSources.add( factory );
        
        NeuronTraceVtxAttribMgr attribMgr = new NeuronTraceVtxAttribMgr();
        attribMgr.handleRenderBuffers(triangleSources, renderIdToBuffers);
        attribMgr.exportVertices(new File("/Users/fosterl/"), "UnitTest_VtxAttribMgr", triangleSources, 500L);
    }
    
}
