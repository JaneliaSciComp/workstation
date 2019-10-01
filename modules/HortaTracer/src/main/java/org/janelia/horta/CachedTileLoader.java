
package org.janelia.horta;

import java.io.File;
import java.io.FileNotFoundException;
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

        RawTileFileProxy(TileLoader tileLoader, String tileStorageURL, String tileLocation) {
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
        public Long estimateSizeInBytes() {
            fetchContent();
            return streamableContent.getSize();
        }

        @Nullable
        @Override
        public InputStream openContentStream() {
            LOG.debug("Open content stream {} / {}", tileStorageURL, tileLocation);
            fetchContent();
            try {
                return streamableContent.getContent();
            } finally {
                // since the file proxy is being cached we don't want to keep this
                // around once it was consumed because if some other thread tries to read it again the stream pointer most likely will
                // not be where the caller expects it
                streamableContent = null;
            }
        }

        private void fetchContent() {
            if (streamableContent == null) {
                LOG.debug("Fetch content {} / {}", tileStorageURL, tileLocation);
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
                fileKey -> new RawTileFileProxy(delegate, fileKey.tileStorageURL, fileKey.tileLocation),
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
        LOG.debug("Stream tile content {} / {}", storageLocation, tileLocation);
        try {
            FileProxy fp = rawTileFileCache.getCachedFileEntry(new RawTileFileKey(storageLocation, tileLocation), false);
            InputStream contentStream = fp.openContentStream();
            return Streamable.of(contentStream, fp.estimateSizeInBytes());
        } catch (FileNotFoundException e) {
            LOG.error("File not found for {} / {}", storageLocation, tileLocation, e);
            return Streamable.empty();
        }
    }

}