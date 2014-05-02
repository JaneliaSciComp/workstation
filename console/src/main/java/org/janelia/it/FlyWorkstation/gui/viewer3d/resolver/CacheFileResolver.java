package org.janelia.it.FlyWorkstation.gui.viewer3d.resolver;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.shared.loader.file_resolver.FileResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

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
            cachedFile = SessionMgr.getCachedFile( fileName, false );
        } catch ( Throwable ex ) {
            logger.warn( "Failed to use session manager to resolve file " + fileName + ", returning as-is." );
        }

        if ( cachedFile != null )
            return cachedFile.getAbsolutePath();
        else
            return fileName;
    }
}
