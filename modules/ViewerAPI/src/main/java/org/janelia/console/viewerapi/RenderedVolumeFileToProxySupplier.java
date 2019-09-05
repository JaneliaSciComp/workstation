package org.janelia.console.viewerapi;

import java.util.function.Supplier;

import org.janelia.filecacheutils.FileKeyToProxySupplier;
import org.janelia.filecacheutils.FileProxy;

class RenderedVolumeFileToProxySupplier implements FileKeyToProxySupplier<RenderedVolumeFileKey> {
    @Override
    public Supplier<FileProxy> getProxyFromKey(RenderedVolumeFileKey fileKey) {
        return fileKey.getFileProxySupplier();
    }
}
