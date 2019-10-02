package org.janelia.console.viewerapi;

import java.io.FileNotFoundException;

import org.janelia.filecacheutils.FileProxy;

class RenderedVolumeFileToProxyMapperImpl implements RenderedVolumeFileToProxyMapper {
    @Override
    public FileProxy getProxyFromKey(RenderedVolumeFileKey fileKey) throws FileNotFoundException {
        return fileKey.getFileProxyMapperDelegate().getProxyFromKey(fileKey);
    }
}
