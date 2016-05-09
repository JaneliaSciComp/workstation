package org.janelia.it.workstation.gui.browser.model;

import org.janelia.it.jacs.model.domain.DomainObject;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectPermission {
    
    private final DomainObject domainObject;
    private final String subjectKey;
    private final boolean owner;
    private boolean read;
    private boolean write;
    
    public DomainObjectPermission(DomainObject domainObject, String subjectKey) {
        this.domainObject = domainObject;
        this.subjectKey = subjectKey;
        this.owner = domainObject.getOwnerKey().equals(subjectKey);
        this.write = domainObject.getWriters().contains(subjectKey);
        this.read = domainObject.getReaders().contains(subjectKey);
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public void setWrite(boolean write) {
        this.write = write;
    }
    
    public DomainObject getDomainObject() {
        return domainObject;
    }

    public String getSubjectKey() {
        return subjectKey;
    }

    public boolean isOwner() {
        return owner;
    }

    public boolean isRead() {
        return read;
    }

    public boolean isWrite() {
        return write;
    }

    public String getPermissions() {
        StringBuilder sb = new StringBuilder();
        if (read) {
            sb.append("r");
        }
        if (write) {
            sb.append("w");
        }
        return sb.toString();
    }
}
