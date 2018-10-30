package org.janelia.it.workstation.gui.large_volume_viewer;

import org.janelia.it.jacs.shared.lvv.BlockTiffOctreeLoadAdapter;
import org.janelia.it.jacs.shared.lvv.TextureData2d;
import org.janelia.it.jacs.shared.lvv.TileIndex;


public class TileStackCacheController extends BlockTiffOctreeLoadAdapter {

    private static TileStackCacheController instance;
    
    public static TileStackCacheController getInstance() {
        return instance;
    }

    public static TileStackCacheController createInstance(BlockTiffOctreeLoadAdapter blockKeyTileLoader) {
        instance = new TileStackCacheController(blockKeyTileLoader);
        return instance;
    }

    private final CachedBlockTiffOctreeLoadAdapter cachedTilesLoader;

    private TileStackCacheController(BlockTiffOctreeLoadAdapter blockKeyTileLoader) {
        super(blockKeyTileLoader.getTileFormat(), blockKeyTileLoader.getVolumeBaseURI());
        this.cachedTilesLoader = new CachedBlockTiffOctreeLoadAdapter(blockKeyTileLoader);
    }

    @Override
    public void loadMetadata() {
        cachedTilesLoader.loadMetadata();
    }

    @Override
    public TextureData2d loadToRam(TileIndex tileIndex)
            throws TileLoadError, MissingTileException  {
        return cachedTilesLoader.loadToRam(tileIndex);
    }

    public boolean hasTile(TileIndex tileIndex) {
        return cachedTilesLoader.hasTile(tileIndex);
    }
}
