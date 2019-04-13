package org.janelia.horta.loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author Christopher Bruns
 */
public class GZIPFileLoader implements FileTypeLoader
{

    @Override
    public boolean supports(DataSource source)
    {
        String ext = FilenameUtils.getExtension(source.getFileName()).toUpperCase();
        if (ext.equals("GZ"))
            return true;
        if (ext.equals("Z"))
            return true;
        return false;
    }

    @Override
    public boolean load(DataSource source, FileHandler handler) throws IOException
    {
        // Delegate to uncompressed datasource
        InputStream uncompressedStream = new GZIPInputStream(source.getInputStream());
        String uncompressedName = FilenameUtils.getBaseName(source.getFileName());
        DataSource uncompressed = new BasicDataSource(uncompressedStream, uncompressedName);
        return handler.handleDataSource(uncompressed);
    }
    
}
