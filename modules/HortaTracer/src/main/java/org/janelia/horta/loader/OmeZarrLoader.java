package org.janelia.horta.loader;

import org.apache.commons.io.FilenameUtils;
import org.janelia.horta.NeuronTracerTopComponent;

import java.io.IOException;

/**
 * For drag and drop.  Simply passes on the folder for input to OmeZarrVolumeBrickSource.
 */
public class OmeZarrLoader implements FileTypeLoader {
    private final NeuronTracerTopComponent neuronTracerTopComponent;

    public OmeZarrLoader(NeuronTracerTopComponent neuronTracerTopComponent) {
        this.neuronTracerTopComponent = neuronTracerTopComponent;
    }

    @Override
    public boolean supports(DataSource source)
    {
        return FilenameUtils.getExtension(source.getFileName()).equalsIgnoreCase("zarr");
    }

    @Override
    public boolean load(final DataSource source, FileHandler handler) throws IOException
    {
        if (source instanceof FileDataSource) {
            return neuronTracerTopComponent.loadDroppedOmeZarr(((FileDataSource)source).getFile().getAbsolutePath());
        }

        return false;
    }
}
