package org.janelia.horta.omezarr;

import org.janelia.jacsstorage.clients.api.JadeStorageService;
import org.janelia.jacsstorage.clients.api.StorageLocation;
import org.janelia.jacsstorage.clients.api.StorageObject;
import org.janelia.jacsstorage.clients.api.StorageObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class OmeZarrJadeReader {
    private final static Logger log = LoggerFactory.getLogger(OmeZarrJadeReader.class);

    protected static final String attributesFile = ".zattrs";

    protected final JadeStorageService jadeStorage;

    protected final StorageLocation storageLocation;

    protected final String basePath;

    public OmeZarrJadeReader(final JadeStorageService jadeStorage, final String basePath) throws IOException {
        this.jadeStorage = jadeStorage;
        this.basePath = basePath;
        this.storageLocation = jadeStorage.getStorageLocationByPath(basePath);

        if (storageLocation == null) {
            throw new IOException("Could not find Jade location for path: " + basePath);
        }
    }

    public String getBasePath() {
        return this.basePath;
    }

    public InputStream getInputStream(String location) {
        final Path path = Paths.get(basePath, location);
        String relativePath = storageLocation.getRelativePath(path.toString().replace("\\", "/"));
        return jadeStorage.getContent(storageLocation, relativePath);
    }

    public InputStream getAttributesStream() {
        final Path path = Paths.get(basePath, attributesFile);
        String relativePath = storageLocation.getRelativePath(path.toString().replace("\\", "/"));
        return jadeStorage.getContent(storageLocation, relativePath);
    }

    public String[] list(final String pathName) throws IOException {
        log.trace("list " + pathName);
        final Path path = Paths.get(basePath, pathName);
        String relativePath = storageLocation.getRelativePath(path.toString());

        try (Stream<StorageObject> stream = jadeStorage.getChildren(storageLocation, relativePath).stream()) {
            return stream
                    .filter(a -> a.isCollection())
                    .map(a -> path.relativize(Paths.get(a.getAbsolutePath())).toString())
                    .toArray(n -> new String[n]);
        } catch (StorageObjectNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
