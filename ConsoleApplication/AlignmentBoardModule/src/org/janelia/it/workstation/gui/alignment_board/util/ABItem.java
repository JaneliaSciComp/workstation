package org.janelia.it.workstation.gui.alignment_board.util;

import org.janelia.it.jacs.model.domain.DomainObject;

public abstract class ABItem {

    protected DomainObject domainObject;

    protected ABItem(DomainObject domainObject) {
        this.domainObject = domainObject;
    }

    public String getName() {
        return domainObject.getName();
    }

    public Long getId() {
        return domainObject.getId();
    }

    public String getOwnerKey() {
        return domainObject.getOwnerKey();
    }

    public String getDefaultColor() {
        return null;
    }

    public boolean canWrite(String subjectKey) {
        return domainObject.getWriters().contains(subjectKey);
    }

    public String getMaskPath() {
        return null;
    }

    public String getChanPath() {
        return null;
    }

    public abstract String getType();
}