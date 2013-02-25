package org.janelia.it.FlyWorkstation.gui.viewer3d.resolver;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;

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
    @Override
    public String getResolvedFilename(String fileName) {
        File cachedFile = SessionMgr.getCachedFile( fileName, false );
        if ( cachedFile != null )
            return cachedFile.getAbsolutePath();
        else
            return null;
    }
}
