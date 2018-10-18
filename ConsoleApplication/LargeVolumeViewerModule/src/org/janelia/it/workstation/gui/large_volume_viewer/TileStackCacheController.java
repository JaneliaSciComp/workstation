package org.janelia.it.workstation.gui.large_volume_viewer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.janelia.it.jacs.shared.lvv.AbstractTextureLoadAdapter;
import org.janelia.it.jacs.shared.lvv.BlockTiffOctreeLoadAdapter;
import org.janelia.it.jacs.shared.lvv.TextureData2d;
import org.janelia.it.jacs.shared.lvv.TileIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class TileStackCacheController extends AbstractTextureLoadAdapter {

    private static Logger LOG = LoggerFactory.getLogger(TileStackCacheController.class);

    private static final Cache<TileIndex, Optional<TextureData2d>> TILE_CACHE = CacheBuilder.newBuilder()
            .maximumSize(200)
            .build();

    private final BlockTiffOctreeLoadAdapter tileLoader;

    public TileStackCacheController(BlockTiffOctreeLoadAdapter tileLoader) {
        super(tileLoader.getTileFormat());
        this.tileLoader = tileLoader;
    }

    @Override
    public TextureData2d loadToRam(TileIndex tileIndex)
            throws TileLoadError, MissingTileException  {
        Optional<TextureData2d> tile = TILE_CACHE.getIfPresent(tileIndex);
        if (tile == null) {
            return retrieveAndCacheTile(tileIndex);
        } else {
            return tile
                    .orElse(null); // tile is missing
        }
    }

    public TextureData2d retrieveAndCacheTile(TileIndex tileIndex)
            throws TileLoadError, MissingTileException {
        TextureData2d tileImage = tileLoader.loadToRam(tileIndex);
        if (tileImage == null) {
            TILE_CACHE.put(tileIndex, Optional.empty());
        } else {
            TILE_CACHE.put(tileIndex, Optional.of(tileImage));
        }
        return tileImage;
    }
}
