package org.janelia.workstation.core.api.services;

import java.io.File;

import org.janelia.workstation.integration.api.FileAccessController;
import org.janelia.workstation.core.api.FileMgr;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = FileAccessController.class, path= FileAccessController.LOOKUP_PATH)
public class ConsoleFileAccessController implements FileAccessController {

    @Override
    public File getCachedFile(String standardPath, boolean forceRefresh) {
        return FileMgr.getFileMgr().getFile(standardPath, forceRefresh);
    }
    
}
