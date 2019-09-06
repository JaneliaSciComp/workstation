
package org.janelia.horta;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.janelia.filecacheutils.FileKey;
import org.janelia.filecacheutils.FileProxy;
import org.janelia.filecacheutils.LocalFileCache;
import org.janelia.filecacheutils.LocalFileCacheStorage;
import org.janelia.rendering.Streamable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JadeBasedRawTileLoader.
 */
public class CachedTileLoader implements TileLoader {

    private static final Logger LOG = LoggerFactory.getLogger(CachedTileLoader.class);

    private static class RawTileFileKey implements FileKey {

        private final String tileStorageURL;
        private final String tileLocation;

        RawTileFileKey(String tileStorageURL, String tileLocation) {
            this.tileStorageURL = tileStorageURL;
            this.tileLocation = tileLocation;
        }

        @Override
        public Path getLocalPath(LocalFileCacheStorage localFileCacheStorage) {
            return localFileCacheStorage.getLocalFileCacheDir().resolve(getLocalCacheEntryName());
        }

        private String getLocalCacheEntryName() {
            final String cachedFileName;
            if (tileLocation.startsWith("jade:///")) {
                cachedFileName = tileLocation.substring("jade:///".length());
            } else if (tileLocation.startsWith("jade://")) {
                cachedFileName = tileLocation.substring("jade://".length());
            } else if (tileLocation.startsWith("/") || tileLocation.startsWith("\\")) {
                cachedFileName = tileLocation.substring(1);
            } else {
                cachedFileName = tileLocation;
            }
            return cachedFileName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            RawTileFileKey that = (RawTileFileKey) o;

            return new EqualsBuilder()
                    .append(tileStorageURL, that.tileStorageURL)
                    .append(tileLocation, that.tileLocation)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(tileStorageURL)
                    .append(tileLocation)
                    .toHashCode();
        }
    }

    private static class RawTileFileProxy implements FileProxy {

        private final TileLoader tileLoader;
        private final String tileStorageURL;
        private final String tileLocation;
        private Streamable<InputStream> streamableContent;

        public RawTileFileProxy(TileLoader tileLoader, String tileStorageURL, String tileLocation) {
            this.tileLoader = tileLoader;
            this.tileStorageURL = tileStorageURL;
            this.tileLocation = tileLocation;
            this.streamableContent = null;
        }

        @Override
        public String getFileId() {
            return tileLocation;
        }

        @Override
        public Optional<Long> estimateSizeInBytes() {
            fetchContent();
            return Optional.of(streamableContent.getSize());
        }

        @Nullable
        @Override
        public InputStream openContentStream() {
            fetchContent();
            return streamableContent.getContent();
        }

        private void fetchContent() {
            if (streamableContent == null) {
                streamableContent = tileLoader.streamTileContent(tileStorageURL, tileLocation);
            }
        }

        @Override
        public File getLocalFile() {
            return null;
        }

        @Override
        public boolean deleteProxy() {
            return false;
        }
    }

    private final LoadingCache<String, Optional<String>> storageURLCache;
    private final LocalFileCache<RawTileFileKey> rawTileFileCache;

    CachedTileLoader(TileLoader delegate,
                     LocalFileCacheStorage localFileCacheStorage,
                     int cacheConcurrency,
                     ExecutorService localCachedFileWriteExecutor) {
        this.storageURLCache = CacheBuilder.newBuilder()
                .maximumSize(10)
                .build(new CacheLoader<String, Optional<String>>() {
                    @Override
                    public Optional<String> load(String key) {
                        return delegate.findStorageLocation(key);
                    }
                })
                ;
        this.rawTileFileCache = new LocalFileCache<>(
                localFileCacheStorage,
                cacheConcurrency,
                fileKey -> () -> new RawTileFileProxy(delegate, fileKey.tileStorageURL, fileKey.tileLocation),
                Executors.newFixedThreadPool(4,
                        new ThreadFactoryBuilder()
                                .setNameFormat("RenderedVolumeFileCacheEvictor-%d")
                                .setDaemon(true).build()),
                localCachedFileWriteExecutor
        );
    }

    @Override
    public Optional<String> findStorageLocation(String tileLocation) {
        try {
            return storageURLCache.get(tileLocation);
        } catch (ExecutionException e) {
            LOG.error("Error retrieving storage URL from {}", tileLocation, e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Streamable<InputStream> streamTileContent(String storageLocation, String tileLocation) {
        FileProxy f = rawTileFileCache.getCachedFileEntry(new RawTileFileKey(storageLocation, tileLocation), false);
        if (f == null) {
            return Streamable.empty();
        } else {
            InputStream contentStream = f.openContentStream();
            if (contentStream == null) {
                return Streamable.empty();
            } else {
                return Streamable.of(contentStream, f.estimateSizeInBytes().orElse(-1L));
            }
        }
    }

}
