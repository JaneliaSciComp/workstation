package org.janelia.it.jacs.model.domain.interfaces;

/**
 * Any object implementing this interface has the option of associated files of specific types. 
 * 
 * The file paths given by getFiles() may be relative to the overall root filepath given by getFilepath().
 *  
 * The best way to deal with these relative filepaths is by using the relevant DomainUtils methods. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface HasRelativeFiles extends HasFilepath, HasFiles {
}
