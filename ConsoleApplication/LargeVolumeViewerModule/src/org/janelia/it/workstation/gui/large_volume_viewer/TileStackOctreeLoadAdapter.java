package org.janelia.it.workstation.gui.large_volume_viewer;

/**
 * Created by murphys on 11/6/2015.
 */
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;
import org.janelia.it.jacs.shared.lvv.*;
import org.janelia.it.jacs.shared.exception.DataSourceInitializeException;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.activity_logging.ActivityLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;

/**
 * Created by murphys on 10/22/2015.
 */
public class TileStackOctreeLoadAdapter extends AbstractTextureLoadAdapter {

    private static Logger log = LoggerFactory.getLogger(TileStackOctreeLoadAdapter.class);
    private ActivityLogHelper activityLog = ActivityLogHelper.getInstance();

    File topFolder;
    String remoteBasePath;
    TileStackCacheController tileStackCacheController=TileStackCacheController.getInstance();
    BlockTiffOctreeLoadAdapter blockTiffOctreeLoadAdapter = new BlockTiffOctreeLoadAdapter();  
    private Long folderOpenTimestamp = 0L;

    //static AtomicInteger ltrCount=new AtomicInteger(0);

    public TileStackOctreeLoadAdapter(String remoteBasePath, File topFolder) throws DataSourceInitializeException {
        super();
        this.remoteBasePath=remoteBasePath;
        this.topFolder=topFolder;
        tileFormat.setIndexStyle(TileIndex.IndexStyle.OCTREE);
        activityLog.logFolderOpen(remoteBasePath, folderOpenTimestamp);
        folderOpenTimestamp = System.currentTimeMillis();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown();
            }
        });

        if (remoteBasePath!=null) {
            blockTiffOctreeLoadAdapter.setRemoteBasePath(remoteBasePath);
        }
        blockTiffOctreeLoadAdapter.setTopFolderAndCoordinateSource(topFolder, new CoordinateToRawTransformFileSource() {
            @Override
            public CoordinateToRawTransform getCoordToRawTransform(String filePath) throws Exception {
                return ModelMgr.getModelMgr().getCoordToRawTransform(filePath);
            }
        });
        tileStackCacheController.setTileFormat(tileFormat);
        tileStackCacheController.setFilesystemConfiguration(remoteBasePath, topFolder);
    }

    private void shutdown() {
        tileStackCacheController.shutdown();
    }

    private int totalMs = 0;
    private int totalNum  = 0;
    private static final int NUM_TILES_TO_AVG = 100;

    @Override
    public TextureData2dGL loadToRam(TileIndex tileIndex) throws TileLoadError, MissingTileException {
        //int count=ltrCount.addAndGet(1);
        //log.info("ltrCount="+count);
        //log.info("loadToRam() useVolumeCache="+VolumeCache.useVolumeCache());
        long startTime = System.nanoTime();
        if (VolumeCache.useVolumeCache()) {

            TextureData2dGL result=tileStackCacheController.loadToRam(tileIndex);
            if (result==null && (!VolumeCache.useVolumeCache())) {
                throw new AbstractTextureLoadAdapter.TileLoadError("VolumeCache off");
            }
            return result;

        } else {

            TextureData2d textureData2d;

            long startTime = System.currentTimeMillis();

            if (HttpDataSource.useHttp()) {
                textureData2d = HttpDataSource.getSample2DTile(tileIndex);
                final double elapsedMs = (double) (System.nanoTime() - startTime) / 1000000.0;
                if (textureData2d != null) {
                    activityLog.logTileLoad(getRelativeSlice(tileIndex), tileIndex, elapsedMs, folderOpenTimestamp);
                }
            } else {
                textureData2d = blockTiffOctreeLoadAdapter.loadToRam(tileIndex);
                final double elapsedMs = (double) (System.nanoTime() - startTime) / 1000000.0;
                if (textureData2d != null) {
                    activityLog.logTileLoad(getRelativeSlice(tileIndex), tileIndex, elapsedMs, folderOpenTimestamp);
                }
            }

            long loadTime = System.currentTimeMillis()-startTime;

            if (log.isDebugEnabled()) {
                synchronized (this) {
                    totalMs += loadTime;
                    totalNum += 1;
                    if (totalNum >= NUM_TILES_TO_AVG) {
                        long avgTime = Math.round((double) totalMs / (double) totalNum);
                        log.debug("loadToRam({} samples) - avgTimeMs={}", NUM_TILES_TO_AVG, avgTime);
                        totalMs = 0;
                        totalNum = 0;
                    }
                }
            }

            //log.info("loadToRam() timeMs="+loadTime);

            if (textureData2d!=null) {
                return new TextureData2dGL(textureData2d);
            } else {
                return null;
            }
        }
    }

    private int getRelativeSlice(TileIndex tileIndex) {
        //long startTime=new Date().getTime();
        int zoomScale = (int)Math.pow(2, tileIndex.getZoom());
        int axisIx = tileIndex.getSliceAxis().index();
        int tileDepth = tileFormat.getTileSize()[axisIx];
        int absoluteSlice = (tileIndex.getCoordinate(axisIx)) / zoomScale;
        int relativeSlice = absoluteSlice % tileDepth;
        return relativeSlice;
    }

}


