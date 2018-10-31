package org.janelia.it.workstation.gui.large_volume_viewer;

import com.google.common.cache.CacheLoader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Supplier;
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
    private static final Long MAX_CACHE_SIZE = ConsoleProperties.getLong("console.localCache.maxSizeBytes", 16L * 1024 * 1024 * 1024);
    private static final Long MAX_CACHE_LENGTH = ConsoleProperties.getLong("console.localCache.maxFiles", 16L);
    private static final String CACHE_FILE_EXT = ".texture";
    private static final Logger LOG = LoggerFactory.getLogger(LocalFileTileCacheLoader.class);

    private static class LocalCache {

        private final NavigableSet<File> localFilesSet = new ConcurrentSkipListSet(new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                long t1 = o1.lastModified();
                long t2 = o2.lastModified();

                if (t1 < t2) {
                    return -1;
                } else if (t1 == t2) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });
        private long cacheSize = 0;

        private void addPath(Path fp) {
            addFile(fp.toFile());
        }

        private void addFile(File f) {
            if (localFilesSet.add(f)) {
                cacheSize += f.length();
            }
        }

        private void touchFile(File f) {
            f.setLastModified(System.currentTimeMillis());
            addFile(f);
        }

        private void addAll(LocalCache other) {
            other.localFilesSet.forEach((f) -> {
                this.addFile(f);
            });
        }

        private boolean removeOldest() {
            if (!localFilesSet.isEmpty()) {
                File oldestTouched = localFilesSet.first();
                if (localFilesSet.remove(oldestTouched)) {
                    cacheSize -= oldestTouched.length();
                }
                try {
                    LOG.debug("Delete {} from cache", oldestTouched);
                    Files.deleteIfExists(oldestTouched.toPath());
                } catch (IOException e) {
                    LOG.warn("Error removing {}", oldestTouched, e);
                }
                return true;
            } else {
                return false;
            }
        }

        private void makeSpaceFor(File newFile) {
            long newFileSize = newFile.length();
            while ((cacheSize + newFileSize > MAX_CACHE_SIZE || localFilesSet.size() > MAX_CACHE_LENGTH) && removeOldest());
        }

    }
    private final Set<TileIndex> currentlyLoadingTiles;
    private final BlockTiffOctreeLoadAdapter tileLoader;
    private final Path localTilesCacheDir;
    private final LocalCache localCache;

    public LocalFileTileCacheLoader(BlockTiffOctreeLoadAdapter tileLoader) {
        this.currentlyLoadingTiles = new LinkedHashSet<>();
        this.tileLoader = tileLoader;
        this.localTilesCacheDir = Paths.get(LOCAL_CACHE_ROOT, CACHE_DIRECTORY_NAME, tileLoader.getVolumeBaseURI().getPath());
        this.localCache = initializeLocalCache(localTilesCacheDir);
    }

    private LocalCache initializeLocalCache(Path cacheDir) {
        Supplier<LocalCache> cacheSupplier = () -> new LocalCache();
        String cachedFilePattern = "glob:**/*" + CACHE_FILE_EXT;
        PathMatcher fileMatcher = FileSystems.getDefault().getPathMatcher(cachedFilePattern);
        try {
            if (Files.exists(cacheDir)) {
                return Files.find(cacheDir, 1, (p, a) -> fileMatcher.matches(p))
                        .collect(cacheSupplier, (fc, f) -> fc.addPath(f), (s1, s2) -> s1.addAll(s2));
            } else {
                return cacheSupplier.get();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Optional<TextureData2d> load(TileIndex tileIndex) {
        try {
            LOG.debug("Loading tile {}", tileIndex);
            currentlyLoadingTiles.add(tileIndex);
            Path sliceImagePath = getSliceImagePathFromTileIndex(tileIndex);
            if (sliceImagePath == null) {
                return Optional.empty();
            } else if (Files.exists(sliceImagePath)) {
                try {
                    localCache.touchFile(sliceImagePath.toFile());
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
                        localCache.makeSpaceFor(sliceImagePath.toFile());
                        localCache.touchFile(sliceImagePath.toFile());
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
        List<String> tileFilePathComponents = FileBasedOctreeMetadataSniffer.getOctreePath(tileIndex, tileLoader.getTileFormat());
        if (tileFilePathComponents == null) {
            return null;
        }
        int sliceNumber = getSliceNumberFromTileIndex(tileIndex);
        String tileOctreePath = tileFilePathComponents.stream().reduce((s1, s2) -> s1 + s2).orElse("0");
        return localTilesCacheDir.resolve("tile_" + tileOctreePath + "_" + sliceNumber + CACHE_FILE_EXT);
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
