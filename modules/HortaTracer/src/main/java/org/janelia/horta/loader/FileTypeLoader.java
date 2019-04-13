package org.janelia.horta.loader;

import java.io.IOException;

/**
 *
 * @author Christopher Bruns
 */
public interface FileTypeLoader
{
    public boolean supports(DataSource source);
    public boolean load(DataSource source, FileHandler handler) throws IOException;
}
