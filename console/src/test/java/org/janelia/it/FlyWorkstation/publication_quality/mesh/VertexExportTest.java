package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import java.io.File;
import java.util.List;

/**
 * Created by fosterl on 4/28/14. Used to test exporting of vertices.
 */

public class VertexExportTest {
    public static void main( String[] args ) throws Exception {
        File outputLoc = new File(System.getProperty("user.dir"));
        String prefix = "VertexExportTestData";
        VtxAttribMgr attribMgr;
        attribMgr = new VtxAttribMgr( MeshRenderTestFacilities.getCompartmentMaskChanRenderableDatas() );
        List<TriangleSource> sources = attribMgr.execute();
//        for ( TriangleSource source: sources ) {
//            source.getVertices();
//            source.getTriangleList();
//        }
        attribMgr.exportVertices( outputLoc, prefix );
        attribMgr.close();

    }
}
