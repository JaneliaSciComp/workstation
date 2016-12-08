package org.janelia.it.jacs.model.domain.interfaces;

import org.janelia.it.jacs.model.domain.enums.FileType;

import java.util.Map;

/**
 * Any object implementing this interface has the option of associated files of specific types. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface HasFiles {
    Map<FileType, String> getFiles();
}
