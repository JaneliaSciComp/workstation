package org.janelia.it.workstation.shared.util;

import java.io.File;

/**
 * A callback parameterized with a file.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class FileCallable extends ParameterizedCallable<File> {
    
    public File getFile() {
        return (File)getParam();
    }
}
