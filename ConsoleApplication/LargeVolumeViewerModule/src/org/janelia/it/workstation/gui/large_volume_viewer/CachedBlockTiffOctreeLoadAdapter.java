package org.janelia.it.workstation.gui.large_volume_viewer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.lvv.BlockTiffOctreeLoadAdapter;
import org.janelia.it.jacs.shared.lvv.TextureData2d;
import org.janelia.it.jacs.shared.lvv.TileIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachedBlockTiffOctreeLoadAdapter extends BlockTiffOctreeLoadAdapter {

    private final static Logger LOG = LoggerFactory.getLogger(CachedBlockTiffOctreeLoadAdapter.class);

    private final ScheduledThreadPoolExecutor tileLoadThreadPool;
    private final LoadingCache<TileIndex, Optional<TextureData2d>> tileCache;

    private final LocalFileTileCacheLoader tileCacheLoader;
    private final BlockTiffOctreeLoadAdapter tileLoader;
    // holds the coord of the loading tiles relative to the current focus
    // this is only for display purposes
    private final Map<TileIndex, int[]> tileCachingMap;
    private final Set<TileIndex> neighborhoodTiles;

    private Double zoom;
    private Vec3 focus;

    public CachedBlockTiffOctreeLoadAdapter(BlockTiffOctreeLoadAdapter tileLoader) {
        super(tileLoader.getTileFormat(), tileLoader.getVolumeBaseURI());
        this.tileLoader = tileLoader;
        this.tileCacheLoader = new LocalFileTileCacheLoader(tileLoader);
        this.tileCache = CacheBuilder.newBuilder()
                .maximumSize(200)
                .build(tileCacheLoader);
        this.tileCachingMap = new LinkedHashMap<>();
        this.tileLoadThreadPool = new ScheduledThreadPoolExecutor(4);
        this.neighborhoodTiles = new HashSet<>();
    }

    @Override
    public void loadMetadata() {
        tileLoader.loadMetadata();
    }

    @Override
    public TextureData2d loadToRam(TileIndex tileIndex)
            throws TileLoadError, MissingTileException  {
        LOG.debug("Load to RAM tile {}", tileIndex);
        if (isEnabled()) {
            return tileCache.getUnchecked(tileIndex)
                    .orElseThrow(() -> new MissingTileException("Tile " + tileIndex + "does not exist"));
        } else {
            return tileLoader.loadToRam(tileIndex);
        }
    }

    private boolean isEnabled() {
        return VolumeCache.useVolumeCache();
    }

    boolean hasTile(TileIndex tileIndex) {
        return tileCache.getIfPresent(tileIndex) != null;
    }

    void setZoom(Double zoom) {
        this.zoom = zoom;
        if (isEnabled()) {
            updateFocusTileIndex();            
        }
    }

    void setFocus(Vec3 focus) {
        this.focus = focus;
        if (isEnabled()) {
            updateFocusTileIndex();
        }
    }

    private void updateFocusTileIndex() {
        if (zoom != null && focus != null) {
            // Update focus tile/stack
            int zoomLevel = tileLoader.getTileFormat().zoomLevelForCameraZoom(zoom);
            TileIndex xyzFocusTileIndex = tileLoader.getTileFormat().tileIndexForXyz(focus, zoomLevel, CoordinateAxis.Z);
            // Must add zoomOutFactor back in for Z to "correct" result from this method
            TileIndex focusTileIndex = new TileIndex(
                    xyzFocusTileIndex.getX(),
                    xyzFocusTileIndex.getY(),
                    ((int) (xyzFocusTileIndex.getZ() * Math.pow(2, zoomLevel))),
                    zoomLevel, 
                    tileLoader.getTileFormat().getZoomLevelCount() - 1,
                    tileLoader.getTileFormat().getIndexStyle(),
                    CoordinateAxis.Z
            );
            neighborhoodTiles.clear();
            tileLoadThreadPool.getQueue().clear();
            tileCache.getUnchecked(focusTileIndex);
            tileCachingMap.clear();
            Stream.of(
                    generateOffsets(0, 0, 0), // focus tile
                    // immediate neighboring tiles
                    generateOffsets(-1, 1, 0), // same level
                    generateOffsets(-1, 1, 1), // 1 level down
                    generateOffsets(-1, 1, -1), // 1 level up
                    // outer tiles
                    generateOffsets(-2, 2, 0), // same level
                    generateOffsets(-2, 2, 1), // 1 level down
                    generateOffsets(-2, 2, -1)) // 1 level up
                    .flatMap(offsets -> offsets)
                    .map(offsets -> ImmutablePair.of(translateTile(focusTileIndex, offsets[0], offsets[1], offsets[2]), offsets))
                    .forEach(tileWithOffsets -> {
                        TileIndex ti = tileWithOffsets.getLeft();
                        int[] offsets = tileWithOffsets.getRight();
                        tileCachingMap.put(ti, offsets);
                        neighborhoodTiles.add(ti);
                        submitCacheTileRequest(ti);
                    });    
        }
    }

    private void submitCacheTileRequest(TileIndex tileIndex) {
       tileLoadThreadPool.schedule(() -> {
           return tileCache.getUnchecked(tileIndex);
       }, 100, TimeUnit.MILLISECONDS);
    }

    private Stream<int[]> generateOffsets(int from, int to, int zOffset) {
        return IntStream.rangeClosed(from, to)
                .mapToObj(yOffset -> yOffset)
                .flatMap(yOffset -> IntStream.rangeClosed(from, to).mapToObj(xOffset -> new int[]{xOffset, yOffset, zOffset}));
    }

    private TileIndex translateTile(TileIndex tile, int xOffset, int yOffset, int zOffset) {
        int zIncrement=zOffset * tileLoader.getTileFormat().getTileSize()[2]*(int)(Math.pow(2, tile.getZoom()));
        int zPosition=tile.getZ()+zIncrement;
        return new TileIndex(
                tile.getX() + xOffset,
                tile.getY() + yOffset,
                zPosition,
                tile.getZoom(),
                tile.getMaxZoom(),
                tile.getIndexStyle(),
                tile.getSliceAxis()
        );
        
    }

    Collection<int[]> getCachingMap() {
        return tileCachingMap.entrySet().stream()
                .map(entry -> {
                    int[] offsets = entry.getValue();
                    TileIndex tileKey = entry.getKey();
                    int status;
                    Optional<TextureData2d> tile = tileCache.getIfPresent(tileKey);
                    if (tile != null) {
                        status = tile.map(td -> 2).orElse(0);
                    } else if (tileCacheLoader.isLoading(tileKey)) {
                        status = 1;
                    } else {
                        status = 0;
                    }
                    return new int[]{offsets[0], offsets[1], offsets[2], status};
                })
                .collect(Collectors.toList());
    }
    
}
