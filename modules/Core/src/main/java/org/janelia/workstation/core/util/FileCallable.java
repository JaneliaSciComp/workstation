package org.janelia.workstation.core.util;

import java.io.File;

/**
 * A callback parameterized with a file.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class FileCallable extends ParameterizedCallable<File> {
    
    public File getFile() {
        return getParam();
    }
}
