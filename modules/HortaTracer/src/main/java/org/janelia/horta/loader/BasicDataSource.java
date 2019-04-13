package org.janelia.horta.loader;

import java.io.InputStream;

/**
 *
 * @author Christopher Bruns
 */
public class BasicDataSource implements DataSource
{
    private final InputStream stream;
    private final String fileName;

    public BasicDataSource(InputStream stream, String fileName) {
        this.stream = stream;
        this.fileName = fileName;
    }
    
    @Override
    public InputStream getInputStream()
    {
        return stream;
    }

    @Override
    public String getFileName()
    {
        return fileName;
    }
    
}
