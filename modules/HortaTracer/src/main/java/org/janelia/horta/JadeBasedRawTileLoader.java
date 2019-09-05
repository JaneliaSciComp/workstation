
package org.janelia.horta;

import java.io.InputStream;
import java.util.Optional;

import org.janelia.rendering.Streamable;
import org.janelia.workstation.core.api.web.JadeServiceClient;

/**
 * JadeBasedRawTileLoader.
 */
public class JadeBasedRawTileLoader implements RawTileLoader {

    private final JadeServiceClient jadeClient;

    public JadeBasedRawTileLoader(JadeServiceClient jadeClient) {
        this.jadeClient = jadeClient;
    }

    @Override
    public Optional<String> findStorageLocation(String tileLocation) {
        return jadeClient.findStorageURL(tileLocation);
    }

    @Override
    public Streamable<InputStream> streamTileContent(String storageLocation, String tileLocation) {
        return jadeClient.streamContent(storageLocation, tileLocation);
    }
}
