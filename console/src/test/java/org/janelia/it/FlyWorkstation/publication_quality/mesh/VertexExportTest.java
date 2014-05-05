package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import org.janelia.it.jacs.model.TestCategories;
import org.janelia.it.jacs.shared.loader.mesh.OBJWriter;
import org.janelia.it.jacs.shared.loader.mesh.VertexAttributeManagerI;
import org.janelia.it.jacs.shared.loader.mesh.VtxAttribMgr;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Created by fosterl on 4/28/14. Used to test exporting of vertices.
 */
@Category(TestCategories.FastTests.class)
public class VertexExportTest {
    public static final long TEST_ID = 500L;
    private static String TEST_STRING =
                    "v -0.5 0.5 0.5\n" +
                    "v 0.5 0.5 0.5\n" +
                    "v 0.5 0.5 -0.5\n" +
                    "v -0.5 0.5 -0.5\n" +
                    "v -0.5 -0.5 0.5\n" +
                    "v -0.5 -0.5 -0.5\n" +
                    "v 0.5 -0.5 0.5\n" +
                    "v 0.5 -0.5 -0.5\n" +
                    "vn -0.57735026 0.57735026 0.57735026\n" +
                    "vn 0.57735026 0.57735026 0.57735026\n" +
                    "vn 0.57735026 0.57735026 -0.57735026\n" +
                    "vn -0.57735026 0.57735026 -0.57735026\n" +
                    "vn -0.57735026 -0.57735026 0.57735026\n" +
                    "vn -0.57735026 -0.57735026 -0.57735026\n" +
                    "vn 0.57735026 -0.57735026 0.57735026\n" +
                    "vn 0.57735026 -0.57735026 -0.57735026\n" +
                    "f 1 2 3\n" +
                    "f 3 4 1\n" +
                    "f 5 1 4\n" +
                    "f 4 6 5\n" +
                    "f 2 1 5\n" +
                    "f 5 7 2\n" +
                    "f 7 5 6\n" +
                    "f 6 8 7\n" +
                    "f 8 6 4\n" +
                    "f 4 3 8\n" +
                    "f 7 8 3\n" +
                    "f 3 2 7\n"
            ;

//    public static void main( String[] args ) throws Exception {
//        File outputLoc = new File(System.getProperty("user.dir"));
//        String prefix = "VertexExportTestData";
//        VertexAttributeManagerI attribMgr;
//        attribMgr = new VtxAttribMgr( MeshRenderTestFacilities.getCompartmentMaskChanRenderableDatas() );
//        //attribMgr = new FewVoxelVtxAttribMgr( 500L );
//        attribMgr.execute();
//        attribMgr.exportVertices( outputLoc, prefix );
//        attribMgr.close();
//
//    }

    @Test
    public void testWithMinimalVoxels() throws Exception {
        // Write the output file.
        File outputLoc = new File(System.getProperty("user.dir"));
        String prefix = "___MinimalVoxelDump";
        VertexAttributeManagerI attribMgr;
        attribMgr = new FewVoxelVtxAttribMgr(TEST_ID, FewVoxelVtxAttribMgr.Scenario.minimal );
        attribMgr.execute();
        attribMgr.exportVertices( outputLoc, prefix );
        attribMgr.close();

        // Read back output file.
        File readBack = new File( outputLoc, prefix + "_" + TEST_ID + OBJWriter.FILE_SUFFIX );
        StringBuilder collector = new StringBuilder();

        BufferedReader br = new BufferedReader( new FileReader( readBack ) );
        String inline = null;
        while ( null != ( inline = br.readLine() ) ) {
            collector.append( inline ).append( "\n" );
        }
        br.close();

        Assert.assertEquals( collector.toString(), TEST_STRING, collector.toString() );
    }
}
