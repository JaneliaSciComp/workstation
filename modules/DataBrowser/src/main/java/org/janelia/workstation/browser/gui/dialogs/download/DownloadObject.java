package org.janelia.workstation.browser.gui.dialogs.download;

import java.util.List;

import org.janelia.model.domain.DomainObject;

public class DownloadObject {
    
    private final DomainObject domainObject;
    private final List<String> folderPath;
    
    public DownloadObject(List<String> folderPath, DomainObject domainObject) {
        this.folderPath = folderPath;
        this.domainObject = domainObject;
    }
    
    public DomainObject getDomainObject() {
        return domainObject;
    }
    
    public List<String> getFolderPath() {
        return folderPath;
    }
}
