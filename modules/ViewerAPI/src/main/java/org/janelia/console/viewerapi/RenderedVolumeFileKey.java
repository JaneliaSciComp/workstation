package org.janelia.console.viewerapi;

import java.nio.file.Path;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.janelia.filecacheutils.FileKey;
import org.janelia.filecacheutils.LocalFileCacheStorage;

public class RenderedVolumeFileKey implements FileKey {
    private final String localName;
    private final RenderedVolumeFileToProxyMapper fileProxyMapperDelegate;

    RenderedVolumeFileKey(String localName, RenderedVolumeFileToProxyMapper fileProxyMapperDelegate)
    {
        this.localName = localName;
        this.fileProxyMapperDelegate = fileProxyMapperDelegate;
    }

    @Override
    public Path getLocalPath(LocalFileCacheStorage localFileCacheStorage) {
        return localFileCacheStorage.getLocalFileCacheDir().resolve(localName);
    }

    String getLocalName() {
        return localName;
    }

    RenderedVolumeFileToProxyMapper getFileProxyMapperDelegate() {
        return fileProxyMapperDelegate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        RenderedVolumeFileKey fileKey = (RenderedVolumeFileKey) o;

        return new EqualsBuilder()
                .append(localName, fileKey.localName)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(localName)
                .toHashCode();
    }
}
