package org.janelia.workstation.core.api.services;

import java.io.File;

import org.janelia.it.jacs.integration.framework.system.FileAccess;
import org.janelia.workstation.core.api.FileMgr;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = FileAccess.class, path=FileAccess.LOOKUP_PATH)
public class ConsoleFileAccess implements FileAccess {

    @Override
    public File getCachedFile(String standardPath, boolean forceRefresh) {
        return FileMgr.getFileMgr().getFile(standardPath, forceRefresh);
    }
    
}
