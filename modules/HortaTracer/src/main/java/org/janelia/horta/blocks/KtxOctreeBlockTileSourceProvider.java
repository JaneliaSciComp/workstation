package org.janelia.horta.blocks;

import com.google.common.base.Preconditions;
import java.net.URL;
import org.janelia.workstation.core.api.web.JadeServiceClient;
import org.janelia.workstation.core.options.ApplicationOptions;
import org.janelia.model.domain.tiledMicroscope.TmSample;

public class KtxOctreeBlockTileSourceProvider {

    public static KtxOctreeBlockTileSource createKtxOctreeBlockTileSource(TmSample sample, URL renderedOctreeUrl) {
        Preconditions.checkArgument(sample.getFilepath() != null && sample.getFilepath().trim().length() > 0);
        if (ApplicationOptions.getInstance().isUseHTTPForTileAccess()) {
            return new JadeKtxOctreeBlockTileSource(new JadeServiceClient(), renderedOctreeUrl).init(sample);
        } else {
            return new FileKtxOctreeBlockTileSource(renderedOctreeUrl).init(sample);
        }

    }

}
