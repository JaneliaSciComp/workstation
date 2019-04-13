package org.janelia.workstation.integration.api;

import java.io.File;

/**
 * Service for unified remote file access.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface FileAccessController {
    
    public static final String LOOKUP_PATH = "FileAccessController/Location/Nodes";

    public File getCachedFile(String standardPath, boolean forceRefresh);
}
