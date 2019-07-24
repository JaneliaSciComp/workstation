package org.janelia.workstation.gui.large_volume_viewer;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import com.google.common.cache.CacheLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 2D TextureData loader for the in memory texture cache.
 */
public class TextureData2dCacheLoader extends CacheLoader<TileIndex, Optional<TextureData2d>> {
    private static final Logger LOG = LoggerFactory.getLogger(TextureData2dCacheLoader.class);

    private final BlockTiffOctreeLoadAdapter delegateTileLoader;
    private final Set<TileIndex> currentlyLoadingTiles;

    TextureData2dCacheLoader(BlockTiffOctreeLoadAdapter delegateTileLoader) {
        this.delegateTileLoader = delegateTileLoader;
        this.currentlyLoadingTiles = new LinkedHashSet<>();
    }

    @Override
    public Optional<TextureData2d> load(TileIndex tileIndex) {
        try {
            LOG.debug("Loading tile {}", tileIndex);
            currentlyLoadingTiles.add(tileIndex);
            TextureData2d sliceImage = delegateTileLoader.loadToRam(tileIndex);
            if (sliceImage == null) {
                return Optional.empty();
            } else {
                return Optional.of(sliceImage);
            }
        } catch (AbstractTextureLoadAdapter.TileLoadError | AbstractTextureLoadAdapter.MissingTileException e) {
            LOG.error("Error loading tile {}", tileIndex, e);
            return Optional.empty();
        } finally {
            currentlyLoadingTiles.remove(tileIndex);
            LOG.debug("Finished loading tile {}", tileIndex);
        }
    }

    boolean isLoading(TileIndex tile) {
        return currentlyLoadingTiles.contains(tile);
    }

}

