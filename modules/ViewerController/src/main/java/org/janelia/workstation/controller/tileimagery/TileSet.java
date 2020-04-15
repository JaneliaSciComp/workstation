package org.janelia.workstation.controller.tileimagery;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A group of Tile2d that together form a complete image on the
 * LargeVolumeViewer, when the RavelerTileServer is used.
 *
 * @author brunsc
 *
 */
public class TileSet extends HashSet<Tile2d> {

    public enum LoadStatus {
        NO_TEXTURES_LOADED,
        SOME_TEXTURES_LOADED,
        COARSE_TEXTURES_LOADED,
        BEST_TEXTURES_LOADED,
    };

    private LoadStatus loadStatus;

    void assignTextures(TextureCache textureCache) {
        int bestCount = 0;
        int coarseCount = 0;
        int noneCount = 0;
        int totalCount = 0;
        for (Tile2d tile : this) {
            tile.assignTexture(textureCache);
            totalCount += 1;
            if (tile.getLoadStatus() == Tile2d.LoadStatus.BEST_TEXTURE_LOADED) {
                bestCount += 1;
            } else if (tile.getLoadStatus() == Tile2d.LoadStatus.COARSE_TEXTURE_LOADED) {
                coarseCount += 1;
            } else {
                noneCount += 1;
            }
        }
        if (totalCount == bestCount) {
            setLoadStatus(LoadStatus.BEST_TEXTURES_LOADED);
        } else if (noneCount == 0) {
            setLoadStatus(LoadStatus.COARSE_TEXTURES_LOADED);
        } else if (noneCount == totalCount) {
            setLoadStatus(LoadStatus.NO_TEXTURES_LOADED);
        } else {
            setLoadStatus(LoadStatus.SOME_TEXTURES_LOADED);
        }
    }

    // 1.0 means fully up-to-date
    // 0.0 means no textures have been loaded
    private float getPercentDisplayable() {
        if (this.size() == 0) {
            return 1.0f; // TODO up-to-date
        }
        float result = 0.0f;
        for (Tile2d tile : this) {
            TileTexture texture = tile.getBestTexture();
            if (texture == null) {
                continue; // no texture at all
            }
            if (texture.getLoadStatus().ordinal() < TileTexture.LoadStatus.RAM_LOADED.ordinal()) {
                continue; // texture is not loaded yet
            }
            if (texture.getIndex().equals(tile.getIndex())) {
                result += 1.0f; // full resolution texture
            } else {
                result += 0.75f; // lower resolution texture
            }
        }
        result /= this.size();
        return result;
    }

    public boolean canDisplay() {
        float pd = getPercentDisplayable();
        // Display if a threshold number of tiles are displayable
        final double minProportion = 0.3;
        return pd >= minProportion;
    }

    Tile2d.LoadStatus getMinStage() {
        if (size() < 1) {
            return Tile2d.LoadStatus.NO_TEXTURE_LOADED;
        }
        Tile2d.LoadStatus result = Tile2d.LoadStatus.BEST_TEXTURE_LOADED; // start optimistic
        for (Tile2d tile : this) {
            Tile2d.LoadStatus stage = tile.getLoadStatus();
            if (stage.ordinal() < result.ordinal()) {
                result = stage;
            }
        }
        return result;
    }

    LoadStatus getLoadStatus() {
        return loadStatus;
    }

    private void setLoadStatus(LoadStatus loadStatus) {
        this.loadStatus = loadStatus;
    }

    // Start loading textures to quickly populate these tiles, even if the
    // textures are not the optimal resolution
    Set<TileIndex> getFastNeededTextures() {
        // Which tiles need to be textured?
        return this.stream()
                .filter(tile -> tile.getLoadStatus().ordinal() < Tile2d.LoadStatus.COARSE_TEXTURE_LOADED.ordinal())
                .map(tile -> tile.getIndex())
                .collect(Collectors.toSet());
    }

    // Start loading textures of the optimal resolution for these tiles
    Set<TileIndex> getBestNeededTextures() {
        // Which tiles need to be textured?
        return this.stream()
                .filter(tile -> tile.getLoadStatus().ordinal() < Tile2d.LoadStatus.BEST_TEXTURE_LOADED.ordinal())
                .map(tile -> tile.getIndex())
                .collect(Collectors.toSet());
    }

}
