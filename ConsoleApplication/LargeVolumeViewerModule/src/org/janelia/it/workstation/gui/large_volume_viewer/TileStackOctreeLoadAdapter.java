package org.janelia.it.workstation.gui.large_volume_viewer;

/**
 * Created by murphys on 11/6/2015.
 */
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;
import org.janelia.it.jacs.shared.lvv.*;
import org.janelia.it.jacs.shared.exception.DataSourceInitializeException;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by murphys on 10/22/2015.
 */
public class TileStackOctreeLoadAdapter extends AbstractTextureLoadAdapter {

    private static Logger log = LoggerFactory.getLogger(TileStackOctreeLoadAdapter.class);

    File topFolder;
    String remoteBasePath;
    TileStackCacheController tileStackCacheController=TileStackCacheController.getInstance();
    BlockTiffOctreeLoadAdapter blockTiffOctreeLoadAdapter = new BlockTiffOctreeLoadAdapter();

    //static AtomicInteger ltrCount=new AtomicInteger(0);

    public TileStackOctreeLoadAdapter(String remoteBasePath, File topFolder) throws DataSourceInitializeException {
        super();
        this.remoteBasePath=remoteBasePath;
        this.topFolder=topFolder;
        tileFormat.setIndexStyle(TileIndex.IndexStyle.OCTREE);

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

    @Override
    public TextureData2dGL loadToRam(TileIndex tileIndex) throws TileLoadError, MissingTileException {
        //int count=ltrCount.addAndGet(1);
        //log.info("ltrCount="+count);

        //log.info("loadToRam() useVolumeCache="+VolumeCache.useVolumeCache());

        if (VolumeCache.useVolumeCache()) {
            TextureData2dGL result=tileStackCacheController.loadToRam(tileIndex);
            if (result==null && (!VolumeCache.useVolumeCache())) {
                throw new AbstractTextureLoadAdapter.TileLoadError("VolumeCache off");
            }
            return result;
        } else {
            return new TextureData2dGL(blockTiffOctreeLoadAdapter.loadToRam(tileIndex));
        }
    }

}


