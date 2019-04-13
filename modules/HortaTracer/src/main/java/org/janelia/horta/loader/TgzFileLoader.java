package org.janelia.horta.loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.FilenameUtils;

/**
 * Delegates contents of tarred, gzipped file as individual file loads.
 * @author Christopher Bruns
 */
public class TgzFileLoader implements FileTypeLoader
{

    @Override
    public boolean supports(DataSource source)
    {
        String ext = FilenameUtils.getExtension(source.getFileName()).toUpperCase();
        if (ext.equals("TGZ"))
            return true;
        return false;    }

    @Override
    public boolean load(DataSource source, FileHandler handler) throws IOException
    {
        // Delegate to uncompressed datasource
        InputStream uncompressedStream = new GZIPInputStream(source.getInputStream());
        // Create extension ".tar", so next delegated layer knows to handle this as a tar file
        String uncompressedName = FilenameUtils.getBaseName(source.getFileName()) + ".tar";
        DataSource uncompressed = new BasicDataSource(uncompressedStream, uncompressedName);
        return handler.handleDataSource(uncompressed);
    }
    
}
