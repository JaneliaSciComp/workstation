
package org.janelia.horta;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.janelia.console.viewerapi.OsFilePathRemapper;
import org.janelia.rendering.Streamable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FileBasedTileLoader.
 */
public class FileBasedTileLoader implements TileLoader {

    private static final Logger LOG = LoggerFactory.getLogger(FileBasedTileLoader.class);

    @Override
    public Optional<String> findStorageLocation(String tileLocation) {
        return Optional.of(tileLocation);
    }

    @Override
    public Streamable<InputStream> streamTileContent(String storageLocation, String tileLocation) {
        Path tilePath = Paths.get(OsFilePathRemapper.remapLinuxPath(tileLocation));

        if (Files.notExists(tilePath)) {
            return Streamable.empty();
        } else {
            try {
                LOG.info("Streaming tile from {}", tilePath);
                return Streamable.of(Files.newInputStream(tilePath), Files.size(tilePath));
            } catch (IOException e) {
                LOG.error("Error opening {}", tileLocation, e);
                throw new IllegalStateException("Error opening " + tileLocation, e);
            }
        }
    }
}
