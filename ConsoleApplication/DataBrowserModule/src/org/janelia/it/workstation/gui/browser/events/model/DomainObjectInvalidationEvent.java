package org.janelia.it.workstation.gui.browser.events.model;

import java.util.ArrayList;
import java.util.Collection;

import org.janelia.it.jacs.model.domain.DomainObject;

/**
 * One or more domain objects have been invalidated. The client receiving this 
 * event should always check isTotalInvalidation first. If true, then all 
 * current objects are being invalidated, and getDomainObjects should not be
 * called because it will return null.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectInvalidationEvent {
    
    private final Collection<DomainObject> domainObjects;
    
    public DomainObjectInvalidationEvent() {
        this.domainObjects = null;
    }
    
    public DomainObjectInvalidationEvent(Collection<? extends DomainObject> domainObjects) {
        this.domainObjects = new ArrayList<>();
        for(DomainObject object : domainObjects) {
            this.domainObjects.add(object);
        }
    }
    
    public Collection<DomainObject> getDomainObjects() {
        return domainObjects;
    }

    public boolean isTotalInvalidation() {
        return domainObjects == null;
    }
}
