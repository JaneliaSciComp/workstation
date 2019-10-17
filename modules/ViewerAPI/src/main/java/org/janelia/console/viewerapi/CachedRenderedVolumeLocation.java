package org.janelia.console.viewerapi;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.janelia.filecacheutils.FileProxy;
import org.janelia.filecacheutils.LocalFileCache;
import org.janelia.filecacheutils.LocalFileCacheStorage;
import org.janelia.rendering.RenderedImageInfo;
import org.janelia.rendering.RenderedVolumeLocation;
import org.janelia.rendering.Streamable;

public class CachedRenderedVolumeLocation implements RenderedVolumeLocation {

    private final RenderedVolumeLocation delegate;
    private final LocalFileCache<RenderedVolumeFileKey> renderedVolumeFileCache;

    public CachedRenderedVolumeLocation(RenderedVolumeLocation delegate,
                                        LocalFileCacheStorage localFileCacheStorage,
                                        int cacheConcurrency,
                                        ExecutorService localCachedFileWriteExecutor) {
        this.delegate = delegate;
        renderedVolumeFileCache = new LocalFileCache<>(
                localFileCacheStorage,
                cacheConcurrency,
                new RenderedVolumeFileToProxyMapperImpl(),
                Executors.newFixedThreadPool(4,
                        new ThreadFactoryBuilder()
                                .setNameFormat("RenderedVolumeFileCacheEvictor-%d")
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
    public String getBaseDataStoragePath() {
        return delegate.getBaseDataStoragePath();
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

    private static class RenderedVolumeContentFileProxy<T> implements FileProxy {
        private String fileId;
        private final Supplier<Streamable<T>> streamableContentSupplier;
        private final Function<T, InputStream> contentToStreamMapper;
        private Streamable<T> streamableContent;

        private RenderedVolumeContentFileProxy(String fileId,
                                               Supplier<Streamable<T>> streamableContentSupplier,
                                               Function<T, InputStream> contentToStreamMapper) {
            this.fileId = fileId;
            this.streamableContentSupplier = streamableContentSupplier;
            this.contentToStreamMapper = contentToStreamMapper;
            this.streamableContent = null;
        }

        @Override
        public String getFileId() {
            return fileId;
        }

        @Override
        public Long estimateSizeInBytes() {
            fetchContent();
            return streamableContent.getSize();
        }

        @Override
        public InputStream openContentStream() throws FileNotFoundException {
            fetchContent();
            try {
                if (streamableContent.getContent() == null) {
                    throw new FileNotFoundException("No content found for " + fileId);
                } else {
                    return contentToStreamMapper.apply(streamableContent.getContent());
                }
            } finally {
                // since the file proxy is being cached we don't want to keep this
                // around once it was consumed because if some other thread tries to read it again the stream pointer most likely will
                // not be where the caller expects it
                streamableContent = null;
            }
        }

        private void fetchContent() {
            if (streamableContent == null) {
                streamableContent = streamableContentSupplier.get();
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
    public Streamable<byte[]> readTiffPageAsTexturedBytes(String imageRelativePath, List<String> channelImageNames, int pageNumber) {
        RenderedVolumeFileKey fileKey = new RenderedVolumeFileKeyBuilder(getBaseDataStoragePath())
                .withRelativePath(imageRelativePath)
                .withChannelImageNames(channelImageNames)
                .withPageNumber(pageNumber)
                .build(renderedVolumeFileKey -> new RenderedVolumeContentFileProxy<>(renderedVolumeFileKey.getLocalName(), () -> delegate.readTiffPageAsTexturedBytes(imageRelativePath, channelImageNames, pageNumber), bytes -> new ByteArrayInputStream(bytes)))
                ;
        return streamableContentFromFileProxy(
                fileKey,
                contentStream -> {
                    try {
                        return ByteStreams.toByteArray(contentStream);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    } finally {
                        try {
                            contentStream.close();
                        } catch (IOException ignore) {
                        }
                    }
                });
   }

    @Override
    public Streamable<byte[]> readTiffImageROIPixels(String imagePath, int xCenter, int yCenter, int zCenter, int dimx, int dimy, int dimz) {
        return delegate.readTiffImageROIPixels(imagePath, xCenter, yCenter, zCenter, dimx, dimy, dimz);
    }

    @Override
    public Streamable<InputStream> getContentFromRelativePath(String relativePath) {
        RenderedVolumeFileKey fileKey = new RenderedVolumeFileKeyBuilder(getBaseDataStoragePath())
                .withRelativePath(relativePath)
                .build(renderedVolumeFileKey -> new RenderedVolumeContentFileProxy<>(renderedVolumeFileKey.getLocalName(), () -> delegate.getContentFromRelativePath(relativePath), Function.identity()))
                ;
        return streamableContentFromFileProxy(fileKey, Function.identity());
    }

    @Override
    public Streamable<InputStream> getContentFromAbsolutePath(String absolutePath) {
        RenderedVolumeFileKey fileKey = new RenderedVolumeFileKeyBuilder(getBaseDataStoragePath())
                .withAbsolutePath(absolutePath)
                .build(renderedVolumeFileKey -> new RenderedVolumeContentFileProxy<>(renderedVolumeFileKey.getLocalName(), () -> delegate.getContentFromAbsolutePath(absolutePath), Function.identity()))
                ;
        return streamableContentFromFileProxy(fileKey, Function.identity());
    }

    private <T> Streamable<T> streamableContentFromFileProxy(RenderedVolumeFileKey fileKey, Function<InputStream, T> streamToContentMapper) {
        try {
            FileProxy fileProxy = renderedVolumeFileCache.getCachedFileEntry(fileKey, false);
            InputStream contentStream = fileProxy.openContentStream();
            return Streamable.of(streamToContentMapper.apply(contentStream), fileProxy.estimateSizeInBytes());
        } catch (FileNotFoundException e) {
            return Streamable.empty();
        }
    }

    @Override
    public boolean checkContentAtRelativePath(String relativePath) {
        return delegate.checkContentAtRelativePath(relativePath);
    }

    @Override
    public boolean checkContentAtAbsolutePath(String absolutePath) {
        return delegate.checkContentAtAbsolutePath(absolutePath);
    }
}
