package org.janelia.horta.loader;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import org.apache.commons.io.FilenameUtils;
import org.janelia.horta.NeuronTracerTopComponent;
import org.openide.util.Exceptions;

/**
 *
 * @author Christopher Bruns
 */
public class TilebaseYamlLoader implements FileTypeLoader
{
    private final NeuronTracerTopComponent nttc;
    
    public TilebaseYamlLoader(NeuronTracerTopComponent nttc) {
        this.nttc = nttc;
    }
    
    @Override
    public boolean supports(DataSource source)
    {
        String ext = FilenameUtils.getExtension(source.getFileName()).toUpperCase();
        if (ext.equals("YML"))
            return true;
        if (ext.equals("YAML"))
            return true;
        return false;
    }

    @Override
    public boolean load(DataSource source, FileHandler handler) throws IOException {
        try (InputStream yamlStream = source.getInputStream()) {
            nttc.loadDroppedYaml(yamlStream);
            return true;
        }
    }
    
}
