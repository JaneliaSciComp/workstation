package org.janelia.it.workstation.gui.viewer3d.resolver;

import java.io.File;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/18/13
 * Time: 10:23 AM
 *
 * Resolve files by calling the session manager and passing through cache.
 */
public class CacheFileResolver implements FileResolver {
    private Logger logger = LoggerFactory.getLogger( CacheFileResolver.class );

    @Override
    public String getResolvedFilename(String fileName) {
        File cachedFile = null;
        try {
            cachedFile = FrameworkImplProvider.getFileAccess().getCachedFile( fileName, false );
        } catch ( Throwable ex ) {
            logger.warn( "Failed to use session manager to resolve file " + fileName + ", returning as-is.",ex);
        }

        if ( cachedFile != null )
            return cachedFile.getAbsolutePath();
        else
            return fileName;
    }
}
