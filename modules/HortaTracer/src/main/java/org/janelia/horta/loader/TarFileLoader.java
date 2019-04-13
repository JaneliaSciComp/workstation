package org.janelia.horta.loader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FilenameUtils;

/**
 * Delegates contents of tar file as individual file loads.
 * @author Christopher Bruns
 */
public class TarFileLoader implements FileTypeLoader
{

    @Override
    public boolean supports(DataSource source)
    {
        String ext = FilenameUtils.getExtension(source.getFileName()).toUpperCase();
        if (ext.equals("TAR"))
            return true;
        if (ext.equals("JAR"))
            return true;
        return false;    }

    @Override
    public boolean load(DataSource source, FileHandler handler) throws IOException
    {
        TarArchiveInputStream tarStream = new TarArchiveInputStream(source.getInputStream());
        TarArchiveEntry entry = null;
        while ( (entry = tarStream.getNextTarEntry()) != null) 
        {
            if (! entry.isFile())
                continue;
            if (entry.isDirectory())
                continue;
            int size = (int)entry.getSize();
            byte[] content = new byte[size];
            tarStream.read(content, 0, size);
            InputStream is = new ByteArrayInputStream(content);
            DataSource entrySource = new BasicDataSource(is, entry.getName());
            handler.handleDataSource(entrySource);
        }
        return true;
    }
    
}
