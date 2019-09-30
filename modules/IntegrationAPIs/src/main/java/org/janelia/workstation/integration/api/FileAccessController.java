package org.janelia.workstation.integration.api;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Service for unified remote file access.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface FileAccessController {
    String LOOKUP_PATH = "FileAccessController/Location/Nodes";

    File getCachedFile(String standardPath, boolean forceRefresh) throws FileNotFoundException;
}
