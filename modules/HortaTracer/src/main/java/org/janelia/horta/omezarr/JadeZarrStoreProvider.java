package org.janelia.horta.omezarr;

import java.io.File;
import java.io.InputStream;

import org.aind.omezarr.zarr.ExternalZarrStore;

public class JadeZarrStoreProvider extends ExternalZarrStore {

    private final OmeZarrJadeReader reader;

    public JadeZarrStoreProvider(String prefix, OmeZarrJadeReader reader) {
        super(prefix);

        this.reader = reader;
    }

    public ExternalZarrStore cloneWithPrefix(String prefix) {
        return new JadeZarrStoreProvider(prefix, reader);
    }

    public boolean exists(String s) {
        return reader.exists(!prefix.isEmpty() ? prefix + File.separator + s : s);
    }

    @Override
    public InputStream getInputStream(String s) {
        return reader.getInputStream(!prefix.isEmpty() ? prefix + File.separator + s : s);
    }
}
