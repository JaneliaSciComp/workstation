package org.janelia.horta.blocks;

import java.io.InputStream;
import java.net.URL;
import org.janelia.workstation.core.api.web.JadeServiceClient;
import org.janelia.model.domain.tiledMicroscope.TmSample;

public class JadeKtxOctreeBlockTileSource extends KtxOctreeBlockTileSource {

    private final JadeServiceClient jadeServiceClient;

    JadeKtxOctreeBlockTileSource(JadeServiceClient jadeServiceClient, URL originatingSampleURL) {
        super(originatingSampleURL);
        this.jadeServiceClient = jadeServiceClient;
    }

    @Override
    protected String getSourceServerURL(TmSample sample) {
        return jadeServiceClient.findStorageURL(sample.getFilepath());
    }
 
    @Override
    protected InputStream streamKeyBlock(KtxOctreeBlockTileKey octreeKey) {
        String octreeKeyBlockPath = getKeyBlockPathURI(octreeKey).toString();
        return jadeServiceClient.streamContent(sourceServerURL, octreeKeyBlockPath);
    }

}
