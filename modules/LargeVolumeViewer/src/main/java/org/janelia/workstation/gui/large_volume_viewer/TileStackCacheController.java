package org.janelia.workstation.gui.large_volume_viewer;

import java.util.Collection;

import org.janelia.it.jacs.shared.geom.Vec3;


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

    public void setZoom(Double zoom) {
        cachedTilesLoader.setZoom(zoom);
    }

    public void setFocus(Vec3 focus) {
        cachedTilesLoader.setFocus(focus);
    }

    public Collection<int[]> getCachingMap() {
        return cachedTilesLoader.getCachingMap();
    }

}
