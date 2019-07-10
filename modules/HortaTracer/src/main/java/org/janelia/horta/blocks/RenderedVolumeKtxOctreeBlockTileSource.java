package org.janelia.horta.blocks;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.janelia.rendering.RenderedVolumeLocation;

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
