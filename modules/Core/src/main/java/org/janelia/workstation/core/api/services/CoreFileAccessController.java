package org.janelia.workstation.core.api.services;

import java.io.File;
import java.io.FileNotFoundException;

import org.janelia.workstation.integration.api.FileAccessController;
import org.janelia.workstation.core.api.FileMgr;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = FileAccessController.class, path=FileAccessController.LOOKUP_PATH)
public class CoreFileAccessController implements FileAccessController {

    @Override
    public File getCachedFile(String standardPath, boolean forceRefresh) throws FileNotFoundException {
        return FileMgr.getFileMgr().getFile(standardPath, forceRefresh).getLocalFile();
    }
    
}
