package org.janelia.horta.loader;

import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.janelia.horta.NeuronTracerTopComponent;
import org.janelia.horta.volume.StaticVolumeBrickSource;

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
        return FilenameUtils.getExtension(source.getFileName()).equalsIgnoreCase(StaticVolumeBrickSource.FileType.ZARR.getExtension());
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
