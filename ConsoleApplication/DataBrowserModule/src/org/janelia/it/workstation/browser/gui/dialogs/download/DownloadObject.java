package org.janelia.it.workstation.browser.gui.dialogs.download;

import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;

public class DownloadObject {
    
    private final DomainObject domainObject;
    private final List<String> path;
    
    public DownloadObject(List<String> path, DomainObject domainObject) {
        this.path = path;
        this.domainObject = domainObject;
    }
    
    public DomainObject getDomainObject() {
        return domainObject;
    }
    
    public List<String> getPath() {
        return path;
    }
}
