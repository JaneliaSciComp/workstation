package org.janelia.it.FlyWorkstation.gui.viewer3d.resolver;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/18/13
 * Time: 10:17 AM
 *
 * Implement this to allow the client to find its files, either trivially or
 * via some agent such as a cache.
 */
public interface FileResolver {
    String getResolvedFilename( String fileName );
}


