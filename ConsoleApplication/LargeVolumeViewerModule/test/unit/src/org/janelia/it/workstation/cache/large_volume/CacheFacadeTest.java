/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

import Jama.Matrix;
import java.io.File;
import org.janelia.it.jacs.model.TestCategories;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.TileIndex;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 *
 * @author fosterl
 */
public class CacheFacadeTest {
    
    public static final int STD_FILE_SIZE = 116137160;
    public static final String VOLUME_LOCATION = "/Volumes/mousebrainmicro/mousebrain_llf/render/TEST_5/";
    private static final double[][] FOCI = {
        {
            74511.17, 47759.29, 19780.44
        },
        {
            74783.5, 47901.14, 19780.44
        },
        {
            74682,74, 47891.21, 19780.44
        }
    };
    
    private static final double[] ZOOMLEVELS = {
        2.0, 1.0, 0.0
    };

    private CacheFacadeI cf;
    
    /*
        public void initCache(URL topFolderURL) {
        try {
            CacheController.getInstance().close();
            tileServer.setPrefetch(false);
            
            final int standardFileLength = CacheFacade.getStandardFileLength(topFolderURL);
            CacheFacade cacheManager = new CacheFacade(standardFileLength);
            log.info("Top Folder URL for Cache is {}, and standard file size is {}.", topFolderURL.getFile(), standardFileLength);
            cacheManager.setNeighborhoodBuilder(
                    new WorldExtentSphereBuilder(sharedVolumeImage, topFolderURL, 500)
            );
            CacheController controller = CacheController.getInstance();
            controller.setManager(cacheManager);
            controller.registerForEvents(camera, sharedVolumeImage);
        } catch (Exception ex) {
            log.error("Failed to open the cache manager.");
            ex.printStackTrace();
        }
    }
    */
    @Before
    public void setup() throws Exception {
        cf = new MapCacheFacade(STD_FILE_SIZE);
        TileFormat tf = mockTileFormat();
        File topFolder = new File(VOLUME_LOCATION);
        cf.setNeighborhoodBuilder(new WorldExtentSphereBuilder(tf, topFolder, 2000));
        CacheController controller = CacheController.getInstance();
        controller.setManager(cf);
    }
    
    @Test
    @Category(TestCategories.FastTests.class)
    public void iterationTest() {
        cf.dumpKeys();
        for ( int i = 0; i < FOCI.length; i++) {
            cf.setCameraZoom(ZOOMLEVELS[i]);
            cf.setFocus(FOCI[i]);
        }
        cf.dumpKeys();
    }
    
    private TileFormat mockTileFormat() {
        // This matches the VOLUME_LOCATION.
		TileFormat tileFormat = new TileFormat();
		tileFormat.setDefaultParameters();
		tileFormat.setVolumeSize(new int[] {2880, 3360, 384});
		tileFormat.setVoxelMicrometers(new double[] {1.0, 1.0, 1.0});
		tileFormat.setTileSize(new int[] {720, 840, 96});
        tileFormat.setOrigin(new int[] { 296330, 189575, 19769 });
        tileFormat.setChannelCount(2);
        tileFormat.setIntensityMax(65535);
        tileFormat.setIntensityMin(0);
        tileFormat.setSrgb(false);
        tileFormat.setHasZSlices(true);
        tileFormat.setBitDepth(16);
        tileFormat.setIndexStyle(TileIndex.IndexStyle.QUADTREE);
        Matrix micronToVox = new Matrix(
                new double[][] {
                    {3.996320055282428, 0.0, 0.0, -296330.0},
                    {0.0, 4.0045718862367865, 0.0, 0.0},
                    {0.0, 0.0, 1.009153336119017, -19769.0},
                    {0.0, 0.0, 0.0, 1.0}
                }
        );
        tileFormat.setMicronToVoxMatrix(micronToVox);
        Matrix voxToMicron = new Matrix(
                new double[][] {
                    {0.2502302083333333, 0.0, 0.0, 74150.71763541666},
                    {0.0, 0.2497145833333335, 0.0, 47339.64213541667},
                    {0.0, 0.0, 0.9909296875, 19589.68889921875},
                    {0.0, 0.0, 0.0, 1.0}
                }
        );
        tileFormat.setVoxToMicronMatrix(voxToMicron);
        tileFormat.setVoxelMicrometers(new double[] {0.2502302083333333, 0.2497145833333335, 0.990929875});
        
		tileFormat.setZoomLevelCount(3);
        
        return tileFormat;
    }
}
