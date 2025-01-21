package org.janelia.workstation.controller.tileimagery;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachedRenderedVolumeLocation implements RenderedVolumeLocation {

    private static final Logger LOG = LoggerFactory.getLogger(CachedRenderedVolumeLocation.class);

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
        private final Supplier<Boolean> contentCheckMapper;
        private Long length;
        private Streamable<T> streamableContent;

        private RenderedVolumeContentFileProxy(String fileId,
                                               Supplier<Streamable<T>> streamableContentSupplier,
                                               Function<T, InputStream> contentToStreamMapper,
                                               Supplier<Boolean> contentCheckMapper) {
            this.fileId = fileId;
            this.streamableContentSupplier = streamableContentSupplier;
            this.contentToStreamMapper = contentToStreamMapper;
            this.contentCheckMapper = contentCheckMapper;
            this.length = null;
            this.streamableContent = null;
        }

        @Override
        public String getFileId() {
            return fileId;
        }

        @Override
        public Long estimateSizeInBytes(boolean alwaysCheck) {
            if (length == null) {
                fetchContent();
            }
            return length;
        }

        @Override
        public InputStream openContentStream(boolean alwaysDownload) throws FileNotFoundException {
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
                length = streamableContent.getSize();
            }
        }

        @Override
        public File getLocalFile(boolean alwaysDownload) {
            return null;
        }

        @Override
        public boolean exists(boolean alwaysCheck) {
            return contentCheckMapper.get();
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
                .build(renderedVolumeFileKey -> new RenderedVolumeContentFileProxy<>(
                        renderedVolumeFileKey.getLocalName(),
                        () -> delegate.readTiffPageAsTexturedBytes(imageRelativePath, channelImageNames, pageNumber),
                        bytes -> new ByteArrayInputStream(bytes),
                        () -> delegate.checkContentAtRelativePath(imageRelativePath)))
                ;
        return streamableContentFromFileProxy(
                fileKey,
                contentStream -> {
                    try {
                        return ByteStreams.toByteArray(contentStream);
                    } catch (FileNotFoundException e) {
                        if (LOG.isDebugEnabled()) {
                            LOG.error("File not found for {}/{} page {}", imageRelativePath, channelImageNames, pageNumber, e);
                        }
                        return null;
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    } finally {
                        try {
                            contentStream.close();
                        } catch (IOException ignore) {
                        }
                    }
                },
                false);
   }

    @Override
    public Streamable<byte[]> readTiffImageROIPixels(String imagePath, int xCenter, int yCenter, int zCenter, int dimx, int dimy, int dimz) {
        return delegate.readTiffImageROIPixels(imagePath, xCenter, yCenter, zCenter, dimx, dimy, dimz);
    }

    @Override
    public String getContentURIFromRelativePath(String relativePath) {
        return delegate.getContentURIFromRelativePath(relativePath);
    }

    @Override
    public String getContentURIFromAbsolutePath(String absolutePath) {
        return delegate.getContentURIFromAbsolutePath(absolutePath);
    }

    @Override
    public Streamable<InputStream> getContentFromRelativePath(String relativePath) {
        RenderedVolumeFileKey fileKey = new RenderedVolumeFileKeyBuilder(getBaseDataStoragePath())
                .withRelativePath(relativePath)
                .build(renderedVolumeFileKey -> new RenderedVolumeContentFileProxy<>(
                        renderedVolumeFileKey.getLocalName(),
                        () -> delegate.getContentFromRelativePath(relativePath),
                        Function.identity(),
                        () -> delegate.checkContentAtRelativePath(relativePath)))
                ;
        return streamableContentFromFileProxy(fileKey, Function.identity(), false);
    }

    @Override
    public Streamable<InputStream> getContentFromAbsolutePath(String absolutePath) {
        RenderedVolumeFileKey fileKey = new RenderedVolumeFileKeyBuilder(getBaseDataStoragePath())
                .withAbsolutePath(absolutePath)
                .build(renderedVolumeFileKey -> new RenderedVolumeContentFileProxy<>(
                        renderedVolumeFileKey.getLocalName(),
                        () -> delegate.getContentFromAbsolutePath(absolutePath),
                        Function.identity(),
                        () -> delegate.checkContentAtAbsolutePath(absolutePath)))
                ;
        return streamableContentFromFileProxy(fileKey, Function.identity(), false);
    }

    private <T> Streamable<T> streamableContentFromFileProxy(RenderedVolumeFileKey fileKey, Function<InputStream, T> streamToContentMapper, boolean alwaysDownload) {
        try {
            FileProxy fileProxy = renderedVolumeFileCache.getCachedFileEntry(fileKey, false);
            InputStream contentStream = fileProxy.openContentStream(alwaysDownload);
            return Streamable.of(streamToContentMapper.apply(contentStream), fileProxy.estimateSizeInBytes(alwaysDownload));
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
