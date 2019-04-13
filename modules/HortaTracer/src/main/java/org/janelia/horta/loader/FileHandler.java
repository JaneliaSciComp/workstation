package org.janelia.horta.loader;

import java.io.IOException;

/**
 *
 * @author Christopher Bruns
 */
public interface FileHandler     
{
    public boolean handleDataSource(DataSource source) throws IOException;
}
