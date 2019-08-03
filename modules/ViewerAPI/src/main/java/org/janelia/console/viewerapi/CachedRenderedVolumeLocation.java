package org.janelia.console.viewerapi;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import com.google.common.io.ByteStreams;

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
                                        LocalFileCacheStorage localFileCacheStorage) {
        this.delegate = delegate;
        renderedVolumeFileCache = new LocalFileCache<>(
                localFileCacheStorage,
                new RenderedVolumeFileToProxySupplier(),
                Executors.newFixedThreadPool(4)
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

    @Nullable
    @Override
    public byte[] readTileImagePageAsTexturedBytes(String tileRelativePath, List<String> channelImageNames, int pageNumber) {
        RenderedVolumeFileKey fileKey = new RenderedVolumeFileKeyBuilder(getRenderedVolumePath())
                .withRelativePath(tileRelativePath)
                .withChannelImageNames(channelImageNames)
                .withPageNumber(pageNumber)
                .build(() -> new FileProxy() {
                    private Long size = null;
                    private byte[] textureBytes;

                    @Override
                    public String getFileId() {
                        return tileRelativePath + "." + pageNumber;
                    }

                    @Override
                    public Optional<Long> estimateSizeInBytes() {
                        fetchContent();
                        return Optional.of(size);
                    }

                    @Override
                    public InputStream openContentStream() {
                        fetchContent();
                        if (textureBytes == null) {
                            return null;
                        } else {
                            return new ByteArrayInputStream(textureBytes);
                        }
                    }

                    private void fetchContent() {
                        if (size == null) {
                            textureBytes = delegate.readTileImagePageAsTexturedBytes(tileRelativePath, channelImageNames, pageNumber);
                            if (textureBytes == null) {
                                size = 0L;
                            } else {
                                size = (long) textureBytes.length;
                            }
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
                });
        FileProxy f = renderedVolumeFileCache.getCachedFileEntry(fileKey, false);
        InputStream contentStream;
        if (f != null) {
            contentStream = f.openContentStream();
        } else {
            contentStream = null;
        }
        if (contentStream != null) {
            try {
                return ByteStreams.toByteArray(contentStream);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } finally {
                try {
                    contentStream.close();
                } catch (IOException ignore) {
                }
            }
        } else {
            return null;
        }
   }

    @Nullable
    @Override
    public byte[] readRawTileROIPixels(RawImage rawImage, int channel, int xCenter, int yCenter, int zCenter, int dimx, int dimy, int dimz) {
        return delegate.readRawTileROIPixels(rawImage, channel, xCenter, yCenter, zCenter, dimx, dimy, dimz);
    }

    @Override
    public Optional<StreamableContent> getContentFromRelativePath(String relativePath) {
        RenderedVolumeFileKey fileKey = new RenderedVolumeFileKeyBuilder(getRenderedVolumePath())
                .withRelativePath(relativePath)
                .build(() -> new FileProxy() {
                    private StreamableContent streamableContent = null;

                    @Override
                    public String getFileId() {
                        return getRenderedVolumePath() + relativePath;
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
                        return streamableContent.getStream();
                    }

                    private void fetchContent() {
                        if (streamableContent == null) {
                            streamableContent = delegate.getContentFromRelativePath(relativePath)
                                    .orElse(new StreamableContent(0, null));
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
                });
        return streamableContentFromFileProxy(renderedVolumeFileCache.getCachedFileEntry(fileKey, false));
    }

    @Override
    public Optional<StreamableContent> getContentFromAbsolutePath(String absolutePath) {
        RenderedVolumeFileKey fileKey = new RenderedVolumeFileKeyBuilder(getRenderedVolumePath())
                .withAbsolutePath(absolutePath)
                .build(() -> new FileProxy() {
                    private StreamableContent streamableContent;

                    @Override
                    public String getFileId() {
                        return getRenderedVolumePath() + absolutePath;
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
                        return streamableContent.getStream();
                    }

                    private void fetchContent() {
                        if (streamableContent == null) {
                            streamableContent = delegate.getContentFromAbsolutePath((absolutePath))
                                    .orElse(new StreamableContent(0, null));
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
                });
        return streamableContentFromFileProxy(renderedVolumeFileCache.getCachedFileEntry(fileKey, false));
    }

    private Optional<StreamableContent> streamableContentFromFileProxy(FileProxy f) {
        if (f == null) {
            return Optional.empty();
        } else {
            return Optional.of(new StreamableContent(f.estimateSizeInBytes().orElse(-1L), f.openContentStream()));
        }
    }
}
