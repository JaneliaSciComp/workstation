package org.janelia.horta.omezarr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.aind.omezarr.zarr.ExternalZarrStore;

public class JadeZarrStoreProvider extends ExternalZarrStore {

    private final OmeZarrJadeReader reader;

    public JadeZarrStoreProvider(String prefix, OmeZarrJadeReader reader)
    {
        super(prefix);

        this.reader = reader;
    }

    @Override
    public InputStream getInputStream(String s) throws IOException {
        return reader.getInputStream(prefix.length() > 0 ? prefix + File.separator + s  :s);
    }

    @Override
    public OutputStream getOutputStream(String s) throws IOException {
        return null;
    }

    @Override
    public void delete(String s) throws IOException {

    }

    @Override
    public TreeSet<String> getArrayKeys() throws IOException {
        return null;
    }

    @Override
    public TreeSet<String> getGroupKeys() throws IOException {
        return null;
    }

    @Override
    public TreeSet<String> getKeysEndingWith(String s) throws IOException {
        return null;
    }

    @Override
    public Stream<String> getRelativeLeafKeys(String s) throws IOException {
        return null;
    }
}
