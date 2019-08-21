package org.janelia.console.viewerapi;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.janelia.filecacheutils.FileProxy;
import org.janelia.filecacheutils.LocalFileCache;
import org.janelia.filecacheutils.LocalFileCacheStorage;
import org.janelia.rendering.RawImage;
import org.janelia.rendering.RenderedImageInfo;
import org.janelia.rendering.RenderedVolumeLocation;
import org.janelia.rendering.StreamableContent;

public class CachedRenderedVolumeLocation implements RenderedVolumeLocation {

    private final RenderedVolumeLocation delegate;
    private final LocalFileCache<RenderedVolumeFileKey> renderedVolumeFileCache;

    public CachedRenderedVolumeLocation(RenderedVolumeLocation delegate,
                                        LocalFileCacheStorage localFileCacheStorage,
                                        ExecutorService localCachedFileWriteExecutor) {
        this.delegate = delegate;
        renderedVolumeFileCache = new LocalFileCache<>(
                localFileCacheStorage,
                new RenderedVolumeFileToProxySupplier(),
                Executors.newFixedThreadPool(4,
                        new ThreadFactoryBuilder()
                                .setNameFormat("CacheEvictor-%d")
                                .setDaemon(true).build()),
                localCachedFileWriteExecutor
        );
    }

    @Override
    public URI getConnectionURI() {
        return delegate.getConnectionURI();
    }

    @Override
    public URI getDataStorageURI() {
        return delegate.getDataStorageURI();
    }

    @Override
    public String getRenderedVolumePath() {
        return delegate.getRenderedVolumePath();
    }

    @Override
    public List<URI> listImageUris(int level) {
        return delegate.listImageUris(level);
    }

    @Nullable
    @Override
    public RenderedImageInfo readTileImageInfo(String tileRelativePath) {
        return delegate.readTileImageInfo(tileRelativePath);
    }

    private static class RenderedVolumeContentFileProxy implements FileProxy {
        private String fileId;
        private final Supplier<Optional<StreamableContent>> streamableContentSupplier;
        private StreamableContent streamableContent;

        private RenderedVolumeContentFileProxy(String fileId, Supplier<Optional<StreamableContent>> streamableContentSupplier) {
            this.fileId = fileId;
            this.streamableContentSupplier = streamableContentSupplier;
            this.streamableContent = null;
        }

        @Override
        public String getFileId() {
            return fileId;
        }

        @Override
        public Optional<Long> estimateSizeInBytes() {
            fetchContent();
            return Optional.of(streamableContent.getSize());
        }

        @Override
        public InputStream openContentStream() {
            fetchContent();
            return streamableContent.getStream();
        }

        private void fetchContent() {
            if (streamableContent == null) {
                streamableContent = streamableContentSupplier.get().orElse(StreamableContent.empty());
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

    @Override
    public Optional<StreamableContent> readTileImagePageAsTexturedBytes(String tileRelativePath, List<String> channelImageNames, int pageNumber) {
        RenderedVolumeFileKey fileKey = new RenderedVolumeFileKeyBuilder(getRenderedVolumePath())
                .withRelativePath(tileRelativePath)
                .withChannelImageNames(channelImageNames)
                .withPageNumber(pageNumber)
                .build(() -> new RenderedVolumeContentFileProxy(
                        tileRelativePath + "." + pageNumber,
                        () -> delegate.readTileImagePageAsTexturedBytes(tileRelativePath, channelImageNames, pageNumber))
                );
        FileProxy f = renderedVolumeFileCache.getCachedFileEntry(fileKey, false);
        return streamableContentFromFileProxy(renderedVolumeFileCache.getCachedFileEntry(fileKey, false));
   }

    @Override
    public Optional<StreamableContent> readRawTileROIPixels(RawImage rawImage, int channel, int xCenter, int yCenter, int zCenter, int dimx, int dimy, int dimz) {
        return delegate.readRawTileROIPixels(rawImage, channel, xCenter, yCenter, zCenter, dimx, dimy, dimz);
    }

    @Override
    public Optional<StreamableContent> getContentFromRelativePath(String relativePath) {
        RenderedVolumeFileKey fileKey = new RenderedVolumeFileKeyBuilder(getRenderedVolumePath())
                .withRelativePath(relativePath)
                .build(() -> new RenderedVolumeContentFileProxy(
                        getRenderedVolumePath() + relativePath,
                        () -> delegate.getContentFromRelativePath(relativePath))
                );
        return streamableContentFromFileProxy(renderedVolumeFileCache.getCachedFileEntry(fileKey, false));
    }

    @Override
    public Optional<StreamableContent> getContentFromAbsolutePath(String absolutePath) {
        RenderedVolumeFileKey fileKey = new RenderedVolumeFileKeyBuilder(getRenderedVolumePath())
                .withAbsolutePath(absolutePath)
                .build(() -> new RenderedVolumeContentFileProxy(
                        getRenderedVolumePath() + absolutePath,
                        () -> delegate.getContentFromAbsolutePath(absolutePath))
                );
        return streamableContentFromFileProxy(renderedVolumeFileCache.getCachedFileEntry(fileKey, false));
    }

    private Optional<StreamableContent> streamableContentFromFileProxy(FileProxy f) {
        if (f == null) {
            return Optional.empty();
        } else {
            return Optional.of(StreamableContent.of(f.estimateSizeInBytes().orElse(-1L), f.openContentStream()));
        }
    }
}
