
package org.janelia.horta;

import java.io.InputStream;
import java.util.Optional;

import org.janelia.rendering.Streamable;

/**
 * RawTileLoader.
 */
public interface RawTileLoader {
    Optional<String> findStorageLocation(String tileLocation);

    Streamable<InputStream> streamTileContent(String storageLocation, String tileLocation);
}
