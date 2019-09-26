package org.janelia.horta.loader;

import java.io.InputStream;
import java.util.function.Supplier;

/**
 *
 * @author Christopher Bruns
 */
public class BasicDataSource implements DataSource
{
    private final Supplier<InputStream> streamSupplier;
    private final String fileName;

    public BasicDataSource(Supplier<InputStream> streamSupplier, String fileName) {
        this.streamSupplier = streamSupplier;
        this.fileName = fileName;
    }
    
    @Override
    public InputStream openInputStream()
    {
        return streamSupplier.get();
    }

    @Override
    public String getFileName()
    {
        return fileName;
    }
    
}
