package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import org.janelia.it.jacs.compute.access.loader.renderable.MaskChanRenderableData;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import static org.janelia.it.FlyWorkstation.gui.TestingConstants.*;

/**
 * Created by fosterl on 4/11/14.
 */
public class VtxAttribMgrTest {
    private static final int STARTING_TRIANGLE = 17000;
    private static final int ENDING_TRIANGLE = 17030;

    // This test string depends on the starting/ending triangles being the same, as well as the local file paths
    // being as they were when this string was originally generated.
    // This string was generated on 4/14/2014.
    private static final String TEST_STRING = "17000 NORMALs={[X=0.57735026,Y=0.57735026,Z=0.57735026][X=0.57735026,Y=0.57735026,Z=0.57735026][X=0.70710677,Y=0.70710677,Z=0.0]}  COORDs={[X=462.5,Y=297.5,Z=143.5][X=462.5,Y=297.5,Z=142.5][X=462.5,Y=298.5,Z=142.5]}  \n" +
            "17001 NORMALs={[X=0.70710677,Y=0.70710677,Z=0.0][X=0.57735026,Y=0.57735026,Z=0.57735026][X=0.57735026,Y=0.57735026,Z=0.57735026]}  COORDs={[X=462.5,Y=298.5,Z=142.5][X=462.5,Y=298.5,Z=143.5][X=462.5,Y=297.5,Z=143.5]}  \n" +
            "17002 NORMALs={[X=0.0,Y=0.0,Z=-1.0][X=0.0,Y=0.0,Z=-1.0][X=-0.70710677,Y=0.0,Z=-0.70710677]}  COORDs={[X=387.5,Y=319.5,Z=176.5][X=388.5,Y=319.5,Z=176.5][X=388.5,Y=319.5,Z=177.5]}  \n" +
            "17003 NORMALs={[X=-0.70710677,Y=0.0,Z=-0.70710677][X=0.0,Y=0.0,Z=-1.0][X=0.0,Y=0.0,Z=-1.0]}  COORDs={[X=388.5,Y=319.5,Z=177.5][X=387.5,Y=319.5,Z=177.5][X=387.5,Y=319.5,Z=176.5]}  \n" +
            "17004 NORMALs={[X=0.0,Y=0.0,Z=1.0][X=0.0,Y=0.0,Z=1.0][X=0.0,Y=0.0,Z=1.0]}  COORDs={[X=388.5,Y=291.5,Z=114.5][X=388.5,Y=290.5,Z=114.5][X=389.5,Y=290.5,Z=114.5]}  \n" +
            "17005 NORMALs={[X=0.0,Y=0.0,Z=1.0][X=0.0,Y=0.0,Z=1.0][X=0.0,Y=0.0,Z=1.0]}  COORDs={[X=389.5,Y=290.5,Z=114.5][X=389.5,Y=291.5,Z=114.5][X=388.5,Y=291.5,Z=114.5]}  \n" +
            "17006 NORMALs={[X=0.0,Y=0.0,Z=-1.0][X=0.0,Y=0.0,Z=-1.0][X=0.0,Y=0.0,Z=-1.0]}  COORDs={[X=460.5,Y=327.5,Z=178.5][X=461.5,Y=327.5,Z=178.5][X=461.5,Y=327.5,Z=179.5]}  \n" +
            "17007 NORMALs={[X=0.0,Y=0.0,Z=-1.0][X=0.57735026,Y=0.57735026,Z=-0.57735026][X=0.0,Y=0.0,Z=-1.0]}  COORDs={[X=461.5,Y=327.5,Z=179.5][X=460.5,Y=327.5,Z=179.5][X=460.5,Y=327.5,Z=178.5]}  \n" +
            "17008 NORMALs={[X=0.0,Y=0.0,Z=-1.0][X=0.0,Y=0.0,Z=-1.0][X=0.0,Y=0.70710677,Z=-0.70710677]}  COORDs={[X=386.5,Y=351.5,Z=140.5][X=387.5,Y=351.5,Z=140.5][X=387.5,Y=351.5,Z=141.5]}  \n" +
            "17009 NORMALs={[X=0.0,Y=0.70710677,Z=-0.70710677][X=0.57735026,Y=0.57735026,Z=-0.57735026][X=0.0,Y=0.0,Z=-1.0]}  COORDs={[X=387.5,Y=351.5,Z=141.5][X=386.5,Y=351.5,Z=141.5][X=386.5,Y=351.5,Z=140.5]}  \n" +
            "17010 NORMALs={[X=0.0,Y=0.70710677,Z=0.70710677][X=0.0,Y=0.70710677,Z=0.70710677][X=0.0,Y=1.0,Z=0.0]}  COORDs={[X=460.5,Y=328.5,Z=158.5][X=461.5,Y=328.5,Z=158.5][X=461.5,Y=328.5,Z=157.5]}  \n" +
            "17011 NORMALs={[X=0.0,Y=1.0,Z=0.0][X=0.0,Y=1.0,Z=0.0][X=0.0,Y=0.70710677,Z=0.70710677]}  COORDs={[X=461.5,Y=328.5,Z=157.5][X=460.5,Y=328.5,Z=157.5][X=460.5,Y=328.5,Z=158.5]}  \n" +
            "17012 NORMALs={[X=0.0,Y=0.70710677,Z=0.70710677][X=0.0,Y=0.0,Z=1.0][X=0.0,Y=0.0,Z=1.0]}  COORDs={[X=460.5,Y=328.5,Z=158.5][X=460.5,Y=327.5,Z=158.5][X=461.5,Y=327.5,Z=158.5]}  \n" +
            "17013 NORMALs={[X=0.0,Y=0.0,Z=1.0][X=0.0,Y=0.70710677,Z=0.70710677][X=0.0,Y=0.70710677,Z=0.70710677]}  COORDs={[X=461.5,Y=327.5,Z=158.5][X=461.5,Y=328.5,Z=158.5][X=460.5,Y=328.5,Z=158.5]}  \n" +
            "17014 NORMALs={[X=0.0,Y=0.0,Z=1.0][X=0.0,Y=0.0,Z=1.0][X=0.0,Y=0.0,Z=1.0]}  COORDs={[X=388.5,Y=294.5,Z=114.5][X=388.5,Y=293.5,Z=114.5][X=389.5,Y=293.5,Z=114.5]}  \n" +
            "17015 NORMALs={[X=0.0,Y=0.0,Z=1.0][X=0.0,Y=0.0,Z=1.0][X=0.0,Y=0.0,Z=1.0]}  COORDs={[X=389.5,Y=293.5,Z=114.5][X=389.5,Y=294.5,Z=114.5][X=388.5,Y=294.5,Z=114.5]}  \n" +
            "17016 NORMALs={[X=-0.70710677,Y=0.0,Z=-0.70710677][X=-0.70710677,Y=0.0,Z=-0.70710677][X=-0.70710677,Y=0.0,Z=-0.70710677]}  COORDs={[X=387.5,Y=322.5,Z=177.5][X=387.5,Y=323.5,Z=177.5][X=387.5,Y=323.5,Z=176.5]}  \n" +
            "17017 NORMALs={[X=-0.70710677,Y=0.0,Z=-0.70710677][X=-0.70710677,Y=0.0,Z=-0.70710677][X=-0.70710677,Y=0.0,Z=-0.70710677]}  COORDs={[X=387.5,Y=323.5,Z=176.5][X=387.5,Y=322.5,Z=176.5][X=387.5,Y=322.5,Z=177.5]}  \n" +
            "17018 NORMALs={[X=-0.70710677,Y=0.0,Z=-0.70710677][X=0.0,Y=0.0,Z=-1.0][X=0.0,Y=0.0,Z=-1.0]}  COORDs={[X=387.5,Y=322.5,Z=176.5][X=388.5,Y=322.5,Z=176.5][X=388.5,Y=322.5,Z=177.5]}  \n" +
            "17019 NORMALs={[X=0.0,Y=0.0,Z=-1.0][X=-0.70710677,Y=0.0,Z=-0.70710677][X=-0.70710677,Y=0.0,Z=-0.70710677]}  COORDs={[X=388.5,Y=322.5,Z=177.5][X=387.5,Y=322.5,Z=177.5][X=387.5,Y=322.5,Z=176.5]}  \n" +
            "17020 NORMALs={[X=0.57735026,Y=-0.57735026,Z=-0.57735026][X=0.57735026,Y=-0.57735026,Z=-0.57735026][X=0.57735026,Y=-0.57735026,Z=-0.57735026]}  COORDs={[X=462.5,Y=299.5,Z=177.5][X=461.5,Y=299.5,Z=177.5][X=461.5,Y=299.5,Z=176.5]}  \n" +
            "17021 NORMALs={[X=0.57735026,Y=-0.57735026,Z=-0.57735026][X=0.57735026,Y=-0.57735026,Z=-0.57735026][X=0.57735026,Y=-0.57735026,Z=-0.57735026]}  COORDs={[X=461.5,Y=299.5,Z=176.5][X=462.5,Y=299.5,Z=176.5][X=462.5,Y=299.5,Z=177.5]}  \n" +
            "17022 NORMALs={[X=0.57735026,Y=-0.57735026,Z=-0.57735026][X=0.57735026,Y=-0.57735026,Z=-0.57735026][X=0.57735026,Y=-0.57735026,Z=-0.57735026]}  COORDs={[X=461.5,Y=299.5,Z=176.5][X=462.5,Y=299.5,Z=176.5][X=462.5,Y=299.5,Z=177.5]}  \n" +
            "17023 NORMALs={[X=0.57735026,Y=-0.57735026,Z=-0.57735026][X=0.57735026,Y=-0.57735026,Z=-0.57735026][X=0.57735026,Y=-0.57735026,Z=-0.57735026]}  COORDs={[X=462.5,Y=299.5,Z=177.5][X=461.5,Y=299.5,Z=177.5][X=461.5,Y=299.5,Z=176.5]}  \n" +
            "17024 NORMALs={[X=0.57735026,Y=-0.57735026,Z=-0.57735026][X=0.57735026,Y=-0.57735026,Z=-0.57735026][X=0.70710677,Y=0.0,Z=-0.70710677]}  COORDs={[X=462.5,Y=299.5,Z=177.5][X=462.5,Y=299.5,Z=176.5][X=462.5,Y=300.5,Z=176.5]}  \n" +
            "17025 NORMALs={[X=0.70710677,Y=0.0,Z=-0.70710677][X=0.70710677,Y=0.0,Z=-0.70710677][X=0.57735026,Y=-0.57735026,Z=-0.57735026]}  COORDs={[X=462.5,Y=300.5,Z=176.5][X=462.5,Y=300.5,Z=177.5][X=462.5,Y=299.5,Z=177.5]}  \n" +
            "17026 NORMALs={[X=-0.70710677,Y=0.0,Z=-0.70710677][X=-0.57735026,Y=-0.57735026,Z=-0.57735026][X=-0.57735026,Y=-0.57735026,Z=-0.57735026]}  COORDs={[X=388.5,Y=291.5,Z=181.5][X=388.5,Y=292.5,Z=181.5][X=388.5,Y=292.5,Z=180.5]}  \n" +
            "17027 NORMALs={[X=-0.57735026,Y=-0.57735026,Z=-0.57735026][X=-0.70710677,Y=0.0,Z=-0.70710677][X=-0.70710677,Y=0.0,Z=-0.70710677]}  COORDs={[X=388.5,Y=292.5,Z=180.5][X=388.5,Y=291.5,Z=180.5][X=388.5,Y=291.5,Z=181.5]}  \n" +
            "17028 NORMALs={[X=-0.70710677,Y=0.0,Z=-0.70710677][X=0.0,Y=0.0,Z=-1.0][X=0.0,Y=0.0,Z=-1.0]}  COORDs={[X=388.5,Y=291.5,Z=180.5][X=389.5,Y=291.5,Z=180.5][X=389.5,Y=291.5,Z=181.5]}  \n" +
            "17029 NORMALs={[X=0.0,Y=0.0,Z=-1.0][X=-0.70710677,Y=0.0,Z=-0.70710677][X=-0.70710677,Y=0.0,Z=-0.70710677]}  COORDs={[X=389.5,Y=291.5,Z=181.5][X=388.5,Y=291.5,Z=181.5][X=388.5,Y=291.5,Z=180.5]}  \n" +
            "17030 NORMALs={[X=0.0,Y=1.0,Z=0.0][X=0.70710677,Y=0.70710677,Z=0.0][X=0.0,Y=0.70710677,Z=-0.70710677]}  COORDs={[X=460.5,Y=331.5,Z=175.5][X=461.5,Y=331.5,Z=175.5][X=461.5,Y=331.5,Z=174.5]}  \n";

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {

    }

    @Test
    public void execute() throws Exception {
        List<MaskChanRenderableData> beanList = MeshRenderTestFacilities.getCompartmentMaskChanRenderableDatas();
        VtxAttribMgr mgr = new VtxAttribMgr( beanList );
        List<TriangleSource> sources = mgr.execute();

        StringBuilder outputBuilder = new StringBuilder();
        StringBuilder statReportBuilder = new StringBuilder();
        int triNum = 0;
        for (TriangleSource source: sources ) {
            VertexFactory vtxFac = (VertexFactory)source;
            statReportBuilder.append(LOCAL_COMPARTMENT_CHAN_FILE_PATH)
                    .append(" surface/volume voxel count=")
                    .append( vtxFac.getSurfaceToVolumeRatio() )
                    .append( "\n" );
            List<Triangle> triangles = source.getTriangleList();
            for ( Triangle triangle: triangles ) {
                if ( triNum >= STARTING_TRIANGLE  &&  triNum <= ENDING_TRIANGLE ) {
                    outputBuilder.append(triNum);
                    outputBuilder.append(" NORMALs={");
                    for ( VertexInfoBean bean: triangle.getVertices() ) {
                        float[] normalCoords = bean.getAttribute( VertexInfoBean.KnownAttributes.normal.toString() );
                        outputBuilder.append( "[X=").append(normalCoords[ 0 ]).append(",");
                        outputBuilder.append( "Y=").append(normalCoords[ 1 ]).append(",");
                        outputBuilder.append( "Z=").append(normalCoords[ 2 ] );
                        outputBuilder.append("]");
                    }
                    outputBuilder.append("}  ");
                    outputBuilder.append("COORDs={");
                    for ( VertexInfoBean bean: triangle.getVertices() ) {
                        double[] coords = bean.getKey().getPosition();
                        outputBuilder.append( "[X=").append(coords[ 0 ]).append(",");
                        outputBuilder.append( "Y=").append(coords[ 1 ]).append(",");
                        outputBuilder.append( "Z=").append(coords[ 2 ]).append("]");

                    }
                    outputBuilder.append("}  ");
                    outputBuilder.append( "\n" );
                }
                triNum ++;
            }
        }

        System.out.println("Total of " + triNum + " triangles");
        System.out.println(statReportBuilder.toString());
        Assert.assertTrue( outputBuilder.toString(), outputBuilder.toString().equals( TEST_STRING ) );
    }

}
