package org.janelia.horta.loader;

import java.io.InputStream;

/**
 * Generic interface for both file and stream sources.
 * Because not every source is a file,
 * and InputStream does not necessarily store a file name,
 * and important file type information might be in the file name.
 * @author Christopher Bruns
 */
public interface DataSource
{
    InputStream getInputStream(); // Where to get the bytes
    String getFileName(); // Used to help identify file type
}
