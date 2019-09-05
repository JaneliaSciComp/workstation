
package org.janelia.horta;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.janelia.rendering.Streamable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JadeBasedRawTileLoader.
 */
public class FileBasedRawTileLoader implements RawTileLoader {

    private static final Logger LOG = LoggerFactory.getLogger(FileBasedRawTileLoader.class);

    @Override
    public Optional<String> findStorageLocation(String tileLocation) {
        return Optional.of(tileLocation);
    }

    @Override
    public Streamable<InputStream> streamTileContent(String storageLocation, String tileLocation) {
        Path tilePath = Paths.get(tileLocation);

        if (Files.notExists(tilePath)) {
            return Streamable.empty();
        } else {
            try {
                return Streamable.of(Files.newInputStream(tilePath), Files.size(tilePath));
            } catch (IOException e) {
                LOG.error("Error opening {}", tileLocation, e);
                throw new IllegalStateException("Error opening " + tileLocation, e);
            }
        }
    }
}
