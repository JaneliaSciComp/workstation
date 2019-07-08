package org.janelia.console.viewerapi;

import java.nio.file.Path;
import java.util.function.Supplier;

import org.janelia.filecacheutils.FileKey;
import org.janelia.filecacheutils.FileProxy;
import org.janelia.filecacheutils.LocalFileCacheStorage;

public class RenderedVolumeFileKey implements FileKey {
    private final String localName;
    private final Supplier<FileProxy> fileProxySupplier;

    RenderedVolumeFileKey(String localName, Supplier<FileProxy> fileProxySupplier) {
        this.localName = localName;
        this.fileProxySupplier = fileProxySupplier;
    }

    @Override
    public Path getLocalPath(LocalFileCacheStorage localFileCacheStorage) {
        return localFileCacheStorage.getLocalFileCacheDir().resolve(localName);
    }

    Supplier<FileProxy> getFileProxySupplier() {
        return fileProxySupplier;
    }
}
