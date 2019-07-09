package org.janelia.console.viewerapi;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import com.google.common.io.ByteStreams;

import org.janelia.filecacheutils.FileProxy;
import org.janelia.filecacheutils.LocalFileCache;
import org.janelia.filecacheutils.LocalFileCacheStorage;
import org.janelia.rendering.RawImage;
import org.janelia.rendering.RenderedImageInfo;
import org.janelia.rendering.RenderedVolumeLocation;

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
                    @Override
                    public String getFileId() {
                        return tileRelativePath + "." + pageNumber;
                    }

                    @Nullable
                    @Override
                    public Long getSizeInBytes() {
                        return null;
                    }

                    @Override
                    public InputStream getContentStream() {
                        byte[] textureBytes = delegate.readTileImagePageAsTexturedBytes(tileRelativePath, channelImageNames, pageNumber);
                        if (textureBytes == null) {
                            return null;
                        } else {
                            return new ByteArrayInputStream(textureBytes);
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
        InputStream contentStream = f.getContentStream();
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

    @Nullable
    @Override
    public InputStream streamContentFromRelativePath(String relativePath) {
        RenderedVolumeFileKey fileKey = new RenderedVolumeFileKeyBuilder(getRenderedVolumePath())
                .withRelativePath(relativePath)
                .build(() -> new FileProxy() {
                    @Override
                    public String getFileId() {
                        return getRenderedVolumePath() + relativePath;
                    }

                    @Nullable
                    @Override
                    public Long getSizeInBytes() {
                        return null;
                    }

                    @Override
                    public InputStream getContentStream() {
                        return delegate.streamContentFromAbsolutePath(relativePath);
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
        return f.getContentStream();
    }

    @Nullable
    @Override
    public InputStream streamContentFromAbsolutePath(String absolutePath) {
        RenderedVolumeFileKey fileKey = new RenderedVolumeFileKeyBuilder(getRenderedVolumePath())
                .withAbsolutePath(absolutePath)
                .build(() -> new FileProxy() {

                    @Override
                    public String getFileId() {
                        return getRenderedVolumePath() + absolutePath;
                    }

                    @Nullable
                    @Override
                    public Long getSizeInBytes() {
                        return null;
                    }

                    @Override
                    public InputStream getContentStream() {
                        return delegate.streamContentFromAbsolutePath(absolutePath);
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
        return f.getContentStream();
    }
}
