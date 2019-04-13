package org.janelia.workstation.integration.framework.system;

import java.io.File;

/**
 * Service for unified remote file access.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface FileAccess {
    
    public static final String LOOKUP_PATH = "FileAccess/Location/Nodes";

    public File getCachedFile(String standardPath, boolean forceRefresh);
}
