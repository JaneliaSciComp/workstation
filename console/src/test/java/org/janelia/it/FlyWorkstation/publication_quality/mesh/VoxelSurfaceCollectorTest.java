package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.AlignmentBoardSettings;
import static org.janelia.it.FlyWorkstation.gui.TestingConstants.*;

import org.janelia.it.jacs.model.TestCategories;
import org.janelia.it.jacs.shared.loader.MaskChanDataAcceptorI;
import org.janelia.it.jacs.shared.loader.MaskChanMultiFileLoader;
import org.janelia.it.jacs.shared.loader.MaskChanStreamSourceI;
import org.janelia.it.jacs.shared.loader.mesh.VoxelInfoBean;
import org.janelia.it.jacs.shared.loader.mesh.VoxelSurfaceCollector;
import org.janelia.it.jacs.shared.loader.renderable.RenderableBean;
import org.junit.Assert;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Test and exercise the voxel surface collector.
 *
 * Created by fosterl on 3/24/14.
 */
@Category(TestCategories.FastTests.class)
public class VoxelSurfaceCollectorTest {

    private Logger logger = LoggerFactory.getLogger( VoxelSurfaceCollectorTest.class );
    private VoxelSurfaceCollector voxelSurfaceCollector;

    public VoxelSurfaceCollectorTest() throws Exception {
        voxelSurfaceCollector = getVoxelSurfaceCollector();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void findAllVoxelCount() {

        // Here, can check efficacy of the operation.
        Map<Long,Map<Long,Map<Long,VoxelInfoBean>>> voxelMap = voxelSurfaceCollector.getVoxelMap();

        Assert.assertNotNull( "Null voxel map", voxelMap );
        Assert.assertNotSame("Empty voxel map", 0, voxelMap.size());
        Assert.assertNotSame( "Single voxel in map", 1, voxelMap.size() );
        // Need to browse all submaps of all submaps, in order to find true extend of voxels.
        int voxelCount = 0;
        for ( Map<Long,Map<Long,VoxelInfoBean>> yMaps: voxelMap.values() ) {
            for ( Map<Long,VoxelInfoBean> zMap: yMaps.values() ) {
                voxelCount += zMap.size();
            }
        }
        logger.info( String.format( "Got %d voxels in total.", voxelCount ) );
    }

    @Test
    public void findSurfaceVoxels() throws Exception {
        // The main map keys x against y/z maps.
        Map<Long,Map<Long,Map<Long,VoxelInfoBean>>> voxelMap = voxelSurfaceCollector.getVoxelMap();

        Assert.assertNotNull( "Null voxel map", voxelMap );
        Assert.assertNotSame( "Empty voxel map", 0, voxelMap.size() );
        Assert.assertNotSame( "Single voxel in map", 1, voxelMap.size() );

        int exposedVoxelCount = -1;
        for ( int i = 0; i < 5; i++ ) {
            Set<VoxelInfoBean> exposedVoxels = getExposedVoxelSet(voxelMap);
            // How many?
            logger.info("Got {} exposed voxels on run {}.", exposedVoxels.size(), i);
            if ( exposedVoxelCount == -1 ) {
                exposedVoxelCount = exposedVoxels.size();
            }
            else {
                Assert.assertEquals(
                        "Different count of exposed voxels on pass number " + i,
                        exposedVoxelCount, exposedVoxels.size()
                );
            }
        }

    }

    private Set<VoxelInfoBean> getExposedVoxelSet(Map<Long, Map<Long, Map<Long, VoxelInfoBean>>> voxelMap) {
        Set<VoxelInfoBean> exposedVoxels = new HashSet<VoxelInfoBean>();

        // Need browse submaps, exploring neighborhoods.
        for ( Map<Long,Map<Long,VoxelInfoBean>> yMaps: voxelMap.values() ) {
            for ( Map<Long,VoxelInfoBean> zMap: yMaps.values() ) {
                for ( VoxelInfoBean bean: zMap.values() ) {
                    long[][] neighborhood = bean.getNeighborhood();
                    int neighborPos = 0;
                    boolean isExposed = false;
                    for ( long[] neighbor: neighborhood ) {
                        VoxelInfoBean neighborBean = voxelSurfaceCollector.getVoxelBean( neighbor[ 0 ], neighbor[ 1 ], neighbor[ 2 ] );
                        if ( neighborBean == null ) {
                            bean.setExposedFace(neighborPos);
                            isExposed = true;
                        }
                        neighborPos ++;
                    }
                    if ( isExposed ) {
                        exposedVoxels.add( bean );
                    }
                }
            }
        }
        return exposedVoxels;
    }

    private VoxelSurfaceCollector getVoxelSurfaceCollector() throws Exception {
        // Time-of-writing: only thing bean is used for is its tanslated number.
        RenderableBean bean = new RenderableBean();
        bean.setTranslatedNum( 1 );

        MaskChanMultiFileLoader loader = new MaskChanMultiFileLoader();

        AlignmentBoardSettings settings = new AlignmentBoardSettings();
        settings.setShowChannelData( true );
        settings.setGammaFactor( AlignmentBoardSettings.DEFAULT_GAMMA );
        settings.setChosenDownSampleRate(AlignmentBoardSettings.UNSELECTED_DOWNSAMPLE_RATE);

        VoxelSurfaceCollector voxelAcceptor = new VoxelSurfaceCollector();

        loader.setAcceptors( Arrays.<MaskChanDataAcceptorI>asList(voxelAcceptor) );

        MaskChanStreamSourceI streamSource = new MaskChanStreamSourceI() {
            @Override
            public InputStream getMaskInputStream() throws IOException {
                InputStream testMaskStream = this.getClass().getResourceAsStream(COMPARTMENT_MASK_FILE_NAME);
                if ( testMaskStream == null ) {
                    testMaskStream = new FileInputStream(LOCAL_COMPARTMENT_MASK_FILE_PATH);
                    logger.warn("Resorting to hardcoded mask path.");
                }

                return testMaskStream;

            }

            @Override
            public InputStream getChannelInputStream() throws IOException {
                InputStream testChannelStream = this.getClass().getResourceAsStream(COMPARTMENT_CHAN_FILE_NAME);
                if ( testChannelStream == null ) {
                    testChannelStream = new FileInputStream(LOCAL_COMPARTMENT_CHAN_FILE_PATH);
                    logger.warn("Resorting to hardcoded channel path.");
                }

                return testChannelStream;
            }
        };
        loader.read(bean, streamSource);
        return voxelAcceptor;
    }

}
