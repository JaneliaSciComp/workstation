package org.janelia.it.workstation.gui.browser.gui.listview.icongrid;

import org.janelia.it.jacs.model.domain.enums.FileType;

public interface ImageModel<T,S> {

    public T getImageByUniqueId(S id);
    
    public S getImageUniqueId(T imageObject);
    
    public String getImageFilepath(T imageObject);
    
    public String getImageFilepath(T imageObject, FileType fileType);
    
    public Object getImageLabel(T imageObject);
    
}
