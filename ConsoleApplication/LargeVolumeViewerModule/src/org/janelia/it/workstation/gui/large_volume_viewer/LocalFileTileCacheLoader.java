package org.janelia.it.workstation.gui.large_volume_viewer;

import com.google.common.cache.CacheLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.janelia.it.jacs.shared.lvv.AbstractTextureLoadAdapter.MissingTileException;
import org.janelia.it.jacs.shared.lvv.AbstractTextureLoadAdapter.TileLoadError;
import org.janelia.it.jacs.shared.lvv.BlockTiffOctreeLoadAdapter;
import org.janelia.it.jacs.shared.lvv.FileBasedOctreeMetadataSniffer;
import org.janelia.it.jacs.shared.lvv.TextureData2d;
import org.janelia.it.jacs.shared.lvv.TileIndex;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalFileTileCacheLoader extends CacheLoader<TileIndex, Optional<TextureData2d>> {

    private static final String CONSOLE_PREFS_DIR = System.getProperty("user.home") + ConsoleProperties.getString("Console.Home.Path");
    private static final String LOCAL_CACHE_ROOT = ConsoleProperties.getString("console.localCache.rootDirectory", CONSOLE_PREFS_DIR);
    private static final String CACHE_DIRECTORY_NAME = ConsoleProperties.getString("console.localCache.name", ".jacs-file-cache");
    private static final Logger LOG = LoggerFactory.getLogger(LocalFileTileCacheLoader.class);

    private final Set<TileIndex> currentlyLoadingTiles;
    private final BlockTiffOctreeLoadAdapter tileLoader;
    private final Path localTilesCacheDir;

    public LocalFileTileCacheLoader(BlockTiffOctreeLoadAdapter tileLoader) {
        this.currentlyLoadingTiles = new LinkedHashSet<>();
        this.tileLoader = tileLoader;
        this.localTilesCacheDir = Paths.get(LOCAL_CACHE_ROOT, CACHE_DIRECTORY_NAME, tileLoader.getVolumeBaseURI().getPath());
    }

    @Override
    public Optional<TextureData2d> load(TileIndex tileIndex) {
        try {
            currentlyLoadingTiles.add(tileIndex);
            Path sliceImagePath = getSliceImagePathFromTileIndex(tileIndex);
            if (sliceImagePath == null) {
                return Optional.empty();
            } else if (Files.exists(sliceImagePath)) {
                try {
                    return Optional.of(new TextureData2d(Files.readAllBytes(sliceImagePath)));
                } catch (IOException e) {
                    LOG.error("Error loading tile {} from {}", tileIndex, sliceImagePath, e);
                    throw new IllegalStateException(e);
                }
            } else {
                TextureData2d sliceImage = tileLoader.loadToRam(tileIndex);
                if (sliceImage != null) {
                    try {
                        Files.createDirectories(sliceImagePath.getParent());
                        Files.write(sliceImagePath, sliceImage.copyToByteArray());
                        return Optional.of(sliceImage);
                    } catch (IOException e) {
                        LOG.error("Error caching tile {} locally to {}", tileIndex, sliceImagePath, e);
                        throw new IllegalStateException(e);
                    }
                } else {
                    return Optional.empty();
                }
            }
        } catch (TileLoadError | MissingTileException e) {
            LOG.error("Error loading tile {}", tileIndex, e);
            throw new IllegalStateException(e);
        } finally {
            currentlyLoadingTiles.remove(tileIndex);
        }
    }

    private Path getSliceImagePathFromTileIndex(TileIndex tileIndex) {
        Path relativeSlicePath = FileBasedOctreeMetadataSniffer.getOctreeFilePath(tileIndex, tileLoader.getTileFormat());
        if (relativeSlicePath == null) {
            return null;
        }
        int sliceNumber = getSliceNumberFromTileIndex(tileIndex);
        Path slicePath = localTilesCacheDir.resolve(relativeSlicePath);
        return slicePath.resolve(sliceNumber + ".texture");
    }

    private int getSliceNumberFromTileIndex(TileIndex tileIndex) {
        int zoomScale = (int) Math.pow(2, tileIndex.getZoom());
        int axisIx = tileIndex.getSliceAxis().index();
        int tileDepth = tileLoader.getTileFormat().getTileSize()[axisIx];
        int absoluteSlice = (tileIndex.getCoordinate(axisIx)) / zoomScale;
        int relativeSlice = absoluteSlice % tileDepth;
        if (axisIx == 1) {
            // Raveller y is flipped so flip when slicing in Y (right?)
            relativeSlice = tileDepth - relativeSlice - 1;
        }
        return relativeSlice;
    }

}
