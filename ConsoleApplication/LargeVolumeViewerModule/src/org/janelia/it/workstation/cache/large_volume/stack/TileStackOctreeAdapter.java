package org.janelia.it.workstation.cache.large_volume.stack;

import org.janelia.it.workstation.gui.large_volume_viewer.AbstractTextureLoadAdapter;
import org.janelia.it.workstation.gui.large_volume_viewer.OctreeMetadataSniffer;
import org.janelia.it.workstation.gui.large_volume_viewer.TextureData2dGL;
import org.janelia.it.workstation.gui.large_volume_viewer.TileIndex;
import org.janelia.it.workstation.gui.large_volume_viewer.exception.DataSourceInitializeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by murphys on 10/22/2015.
 */
public class TileStackOctreeAdapter extends AbstractTextureLoadAdapter {

    private static Logger log = LoggerFactory.getLogger(TileStackOctreeAdapter.class);

    File topFolder;
    String remoteBasePath;
    TileStackCacheController tileStackCacheController=TileStackCacheController.getInstance();
    static AtomicInteger ltrCount=new AtomicInteger(0);

    public TileStackOctreeAdapter(String remoteBasePath, File topFolder) throws DataSourceInitializeException {
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

        tileStackCacheController.setTileFormat(tileFormat);
        tileStackCacheController.setFilesystemConfiguration(remoteBasePath, topFolder);

    }

    private void shutdown() {
        tileStackCacheController.shutdown();
    }

    @Override
    public TextureData2dGL loadToRam(TileIndex tileIndex) throws TileLoadError, MissingTileException {
        int count=ltrCount.addAndGet(1);
        log.info("ltrCount="+ltrCount);
        return tileStackCacheController.loadToRam(tileIndex);
    }

}
