/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.janelia.it.jacs.shared.mesh_loader.RenderBuffersBean;
import org.janelia.it.jacs.shared.mesh_loader.TriangleSource;
import org.janelia.it.workstation.gui.full_skeleton_view.data_source.AnnotationSkeletonDataSourceI;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyleModel;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

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
        LineEnclosureFactory factory = new LineEnclosureFactory(6, 16.0);
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
        attribMgr.populateNormals(triangleSources, renderIdToBuffers);
        attribMgr.exportVertices(new File("/Users/fosterl/"), "UnitTest_VtxAttribMgr", triangleSources, 500L);
    }
    
}
