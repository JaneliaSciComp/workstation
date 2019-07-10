package org.janelia.horta.blocks;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.janelia.rendering.RenderedVolumeLocation;
import org.janelia.workstation.core.api.web.JadeServiceClient;
import org.janelia.model.domain.tiledMicroscope.TmSample;

public class RenderedVolumeKtxOctreeBlockTileSource extends KtxOctreeBlockTileSource {

    private final RenderedVolumeLocation renderedVolumeLocation;

    public RenderedVolumeKtxOctreeBlockTileSource(RenderedVolumeLocation renderedVolumeLocation, URL originatingSampleURL) {
        super(originatingSampleURL);
        this.renderedVolumeLocation = renderedVolumeLocation;
    }

    @Override
    URI getDataServerURI() {
        return renderedVolumeLocation.getConnectionURI();
    }

    @Override
    protected InputStream streamKeyBlock(KtxOctreeBlockTileKey octreeKey) {
        String octreeKeyBlockRelativePath = getKeyBlockRelativePathURI(octreeKey).toString();
        return renderedVolumeLocation.streamContentFromRelativePath(octreeKeyBlockRelativePath);
    }

}
