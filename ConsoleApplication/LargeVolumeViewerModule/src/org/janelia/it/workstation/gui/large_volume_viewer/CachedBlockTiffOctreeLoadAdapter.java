package org.janelia.it.workstation.gui.large_volume_viewer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.janelia.it.jacs.shared.lvv.BlockTiffOctreeLoadAdapter;
import org.janelia.it.jacs.shared.lvv.TextureData2d;
import org.janelia.it.jacs.shared.lvv.TileIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class CachedBlockTiffOctreeLoadAdapter extends BlockTiffOctreeLoadAdapter {

    private static Logger LOG = LoggerFactory.getLogger(CachedBlockTiffOctreeLoadAdapter.class);

    private final LoadingCache<TileIndex, Optional<TextureData2d>> tileCache;

    private final LocalFileTileCacheLoader tileCacheLoader;
    private final BlockTiffOctreeLoadAdapter tileLoader;

    public CachedBlockTiffOctreeLoadAdapter(BlockTiffOctreeLoadAdapter tileLoader) {
        super(tileLoader.getTileFormat(), tileLoader.getVolumeBaseURI());
        this.tileLoader = tileLoader;
        this.tileCacheLoader = new LocalFileTileCacheLoader(tileLoader);
        this.tileCache = CacheBuilder.newBuilder()
                .maximumSize(200)
                .build(tileCacheLoader);
    }

    @Override
    public void loadMetadata() {
        tileLoader.loadMetadata();
    }

    @Override
    public TextureData2d loadToRam(TileIndex tileIndex)
            throws TileLoadError, MissingTileException  {
        if (VolumeCache.useVolumeCache()) {
            return tileCache.getUnchecked(tileIndex).orElse(null);
        } else {
            return tileLoader.loadToRam(tileIndex);
        }
    }

    boolean hasTile(TileIndex tileIndex) {
        return tileCache.getUnchecked(tileIndex).map(rt -> true).orElse(false);
    }
}
